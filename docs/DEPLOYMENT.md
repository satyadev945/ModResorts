# ModResorts – Deployment Guide (GCP GKE)

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build & Push Docker Image](#build--push-docker-image)
6. [GCP GKE Deployment](#gcp-gke-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Configuration & Environment Variables](#configuration--environment-variables)
9. [Health Checks & Monitoring](#health-checks--monitoring)
10. [Scaling & Rolling Updates](#scaling--rolling-updates)
11. [Troubleshooting](#troubleshooting)
12. [Security Considerations](#security-considerations)

---

## Overview

**ModResorts** is a Java EE 7 web application (WAR) built with Maven and deployed on Apache Tomcat 9.  
It provides resort availability checking, weather information, and customer management features.

| Property | Value |
|---|---|
| Artifact | `modresorts-2.0.0.war` |
| Java Version | 8 |
| Build Tool | Maven 3.8.x |
| Runtime | Apache Tomcat 9 on `eclipse-temurin:8-jdk-alpine` |
| Application Port | `8080` |
| Health Endpoint | `GET /health` |
| Target Platform | GCP GKE |

---

## Prerequisites

### Local Development
- Docker Desktop ≥ 24.x
- Docker Compose ≥ 2.x
- Java 8 JDK (for local Maven builds)
- Maven 3.8.x

### GCP GKE Deployment
- [Google Cloud SDK (gcloud)](https://cloud.google.com/sdk/docs/install) ≥ 450.x
- [kubectl](https://kubernetes.io/docs/tasks/tools/) ≥ 1.28
- A GCP project with billing enabled
- GKE cluster (Standard or Autopilot) already provisioned
- Google Artifact Registry repository (or Docker Hub account)
- IAM roles: `roles/container.developer`, `roles/artifactregistry.writer`

---

## Project Structure

```
comp agentic scan sync/
├── Dockerfile                  # Multi-stage build (Maven builder + Tomcat runtime)
├── .dockerignore               # Excludes target/, .git/, wrapper files, etc.
├── docker-compose.yml          # Single-service local development stack
├── pom.xml                     # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/   # Application source code
│       └── resources/              # ops.json, reservations.json
├── WebContent/                 # Static assets, JSPs, WEB-INF/web.xml
├── kubernetes/
│   ├── namespace.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
├── scripts/
│   ├── build-push.sh           # Linux/macOS build & push
│   ├── build-push.bat          # Windows build & push
│   ├── deploy-image.sh         # Linux/macOS GKE deploy
│   └── deploy-image.bat        # Windows GKE deploy
└── docs/
    └── DEPLOYMENT.md           # This file
```

---

## Local Development with Docker Compose

### 1. Build and start the application

```bash
# From the repository root
docker compose up --build
```

The application will be available at **http://localhost:8080**.

### 2. Environment variable overrides

Create a `.env` file in the repository root:

```dotenv
REDIS_HOST=your-memorystore-host
REDIS_PORT=6379
WUNDERGROUND_API_KEY=your-api-key
```

Then run:

```bash
docker compose --env-file .env up --build
```

### 3. Stop the application

```bash
docker compose down
```

---

## Build & Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
bash scripts/build-push.sh
```

### Windows

```cmd
scripts\build-push.bat
```

The script will prompt you to:
1. Choose a registry (Google Artifact Registry or Docker Hub)
2. Enter registry credentials and repository details
3. Specify an image tag (defaults to `latest`)

#### Example – Google Artifact Registry

```
Select container registry:
  1) Google Artifact Registry
  2) Docker Hub
Enter choice [1]: 1

Enter GCP Project ID: my-gcp-project
Enter GCP Region [us-central1]: us-central1
Enter Artifact Registry repository name [modresorts-repo]: modresorts-repo
Enter image tag [latest]: v2.0.0
```

The resulting image URI will be:
```
us-central1-docker.pkg.dev/my-gcp-project/modresorts-repo/modresorts:v2.0.0
```

---

## GCP GKE Deployment

### Step 1 – Authenticate with GCP

```bash
gcloud auth login
gcloud config set project YOUR_GCP_PROJECT_ID
```

### Step 2 – Enable required APIs

```bash
gcloud services enable container.googleapis.com artifactregistry.googleapis.com
```

### Step 3 – Create an Artifact Registry repository (if not already created)

```bash
gcloud artifacts repositories create modresorts-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="ModResorts container images"
```

### Step 4 – Create a GKE cluster (if not already created)

**Autopilot (recommended):**
```bash
gcloud container clusters create-auto modresorts-autopilot \
  --region=us-central1 \
  --project=YOUR_GCP_PROJECT_ID
```

**Standard:**
```bash
gcloud container clusters create modresorts-cluster \
  --zone=us-central1-a \
  --num-nodes=2 \
  --machine-type=e2-standard-2 \
  --project=YOUR_GCP_PROJECT_ID
```

### Step 5 – Build and push the image

```bash
bash scripts/build-push.sh
# Select option 1 (Artifact Registry) and follow prompts
```

### Step 6 – Deploy to GKE

```bash
chmod +x scripts/deploy-image.sh
bash scripts/deploy-image.sh
```

The script will:
1. Configure `kubectl` for your cluster
2. Substitute all `{{PLACEHOLDER}}` values in the manifests
3. Apply manifests in order: namespace → deployment → service → ingress
4. Wait for the rollout to complete
5. Display the application URL

### Step 7 – Verify the deployment

```bash
kubectl get pods -n modresorts
kubectl get svc -n modresorts
kubectl get ingress -n modresorts
```

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `modresorts` namespace to isolate all application resources.

### deployment.yaml
Deploys 2 replicas of the ModResorts container with:
- **Image**: `{{IMAGE_URI}}` (replaced at deploy time)
- **Resources**: requests `250m CPU / 512Mi RAM`, limits `500m CPU / 1Gi RAM`
- **Liveness probe**: `GET /health` on port 8080 (initial delay 60s)
- **Readiness probe**: `GET /health` on port 8080 (initial delay 30s)
- **Rolling update**: zero-downtime with `maxUnavailable: 0`

### service.yaml
Exposes the deployment internally as a `ClusterIP` service on port 80 → 8080.

### ingress.yaml
Creates a GKE-managed HTTP(S) Load Balancer using the `gce` ingress class.  
Update `host: modresorts.example.com` to your actual domain before deploying.

---

## Configuration & Environment Variables

| Variable | Description | Default |
|---|---|---|
| `JAVA_OPTS` | JVM flags | `-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` |
| `TZ` | Container timezone | `UTC` |
| `REDIS_HOST` | Google Cloud Memorystore (Redis) host | *(required if Redis is used)* |
| `REDIS_PORT` | Redis port | `6379` |
| `WUNDERGROUND_API_KEY` | Weather Underground API key | *(optional)* |

### Using Kubernetes Secrets for sensitive values

```bash
kubectl create secret generic modresorts-secrets \
  --from-literal=WUNDERGROUND_API_KEY=your-api-key \
  --from-literal=REDIS_HOST=your-memorystore-ip \
  -n modresorts
```

Then reference in `deployment.yaml`:
```yaml
env:
  - name: WUNDERGROUND_API_KEY
    valueFrom:
      secretKeyRef:
        name: modresorts-secrets
        key: WUNDERGROUND_API_KEY
```

---

## Health Checks & Monitoring

The application exposes a health endpoint at `GET /health` which returns:

```json
{"status":"UP","application":"modresorts"}
```

This endpoint is used by both Kubernetes liveness and readiness probes.

### Check pod health manually

```bash
kubectl exec -it $(kubectl get pod -n modresorts -l app=modresorts -o jsonpath='{.items[0].metadata.name}') \
  -n modresorts -- wget -qO- http://localhost:8080/health
```

### View application logs

```bash
kubectl logs -f deployment/modresorts -n modresorts
```

---

## Scaling & Rolling Updates

### Manual scaling

```bash
kubectl scale deployment modresorts --replicas=4 -n modresorts
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling update (new image)

```bash
kubectl set image deployment/modresorts \
  modresorts=us-central1-docker.pkg.dev/my-project/modresorts-repo/modresorts:v2.1.0 \
  -n modresorts

kubectl rollout status deployment/modresorts -n modresorts
```

### Rollback

```bash
kubectl rollout undo deployment/modresorts -n modresorts
# Or to a specific revision:
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod stuck in `Pending`
```bash
kubectl describe pod <pod-name> -n modresorts
# Check for resource quota issues or node capacity
```

### Pod in `CrashLoopBackOff`
```bash
kubectl logs <pod-name> -n modresorts --previous
# Check JVM startup errors or missing environment variables
```

### Liveness probe failing
- Ensure the application has fully started (Tomcat + WAR deployment takes ~30-60s)
- Increase `initialDelaySeconds` in `deployment.yaml` if needed
- Verify `/health` returns HTTP 200: `kubectl exec ... -- wget -qO- http://localhost:8080/health`

### Ingress not getting an IP
```bash
kubectl describe ingress modresorts-ingress -n modresorts
# GKE HTTP(S) Load Balancer provisioning can take 5-10 minutes
```

### Image pull errors
```bash
# Ensure the GKE node service account has Artifact Registry read access
gcloud projects add-iam-policy-binding YOUR_PROJECT \
  --member="serviceAccount:YOUR_NODE_SA@YOUR_PROJECT.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.reader"
```

### Out of memory errors
- Increase `JAVA_OPTS` heap: `-Xmx768m`
- Increase Kubernetes memory limit in `deployment.yaml`

---

## Security Considerations

1. **Non-root container**: The application runs as user `modresorts` (UID 1000) inside the container.
2. **Secrets management**: Use Kubernetes Secrets or Google Secret Manager for API keys and passwords. Never hardcode credentials.
3. **Network policies**: Consider adding Kubernetes NetworkPolicy resources to restrict pod-to-pod communication.
4. **Image scanning**: Enable Artifact Registry vulnerability scanning for the `modresorts` repository.
5. **Workload Identity**: Use GKE Workload Identity instead of service account key files for GCP API access.
6. **TLS**: Configure Google-managed SSL certificates via `ManagedCertificate` resource for HTTPS on the Ingress.
7. **Resource limits**: Always set both `requests` and `limits` to prevent noisy-neighbour issues on shared nodes.
8. **Regular updates**: Keep the base image (`eclipse-temurin:8-jdk-alpine`) and Tomcat version up to date to receive security patches.
