# ModResorts – Deployment Guide (Azure AKS)

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build and Push Docker Image](#build-and-push-docker-image)
6. [Azure AKS Deployment](#azure-aks-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Configuration and Environment Variables](#configuration-and-environment-variables)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)

---

## Overview

**ModResorts** is a Java EE 7 web application (WAR packaging) built with Maven and Java 8. It provides resort booking, weather information, and availability checking services. The application is containerized using a multi-stage Docker build and deployed to **Azure Kubernetes Service (AKS)**.

- **Runtime**: IBM WebSphere Liberty (Java EE 7)
- **Base Image (Runtime)**: `eclipse-temurin:8-jre` (explicit)
- **Builder Image**: `maven:3.8.6-openjdk-8-slim`
- **Application Port**: `9080` (Liberty HTTP)
- **Health Endpoint**: `GET /modresorts/health`
- **WAR Artifact**: `target/modresorts-2.0.0.war`

---

## Prerequisites

### Local Development
- Docker Desktop 24.x or later
- Docker Compose v2.x or later
- Java 8 JDK (for local builds outside Docker)
- Maven 3.8.x or later

### Azure AKS Deployment
- Azure CLI (`az`) 2.50.0 or later — [Install](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- `kubectl` 1.27 or later — [Install](https://kubernetes.io/docs/tasks/tools/)
- An active Azure subscription
- An Azure Container Registry (ACR) or Docker Hub account
- An AKS cluster (see setup below)

---

## Project Structure

```
New test container check/
├── Dockerfile                    # Multi-stage build (CSS → Maven → Liberty → eclipse-temurin:8-jre)
├── .dockerignore                 # Excludes build artifacts and wrapper files
├── docker-compose.yml            # Local development (application only)
├── pom.xml                       # Maven build descriptor (Java 8, WAR packaging)
├── src/                          # Java source code
├── WebContent/                   # Web assets (HTML, JSP, CSS, JS)
├── css-build/                    # PurgeCSS + cssnano pipeline
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace: modresorts
│   ├── deployment.yaml           # Deployment (2 replicas, health probes)
│   ├── service.yaml              # ClusterIP service on port 80 → 9080
│   └── ingress.yaml              # Azure Application Gateway Ingress
├── scripts/
│   ├── build-push.sh             # Linux/macOS: build & push to ACR or Docker Hub
│   ├── build-push.bat            # Windows: build & push to ACR or Docker Hub
│   ├── deploy-image.sh           # Linux/macOS: deploy to AKS
│   └── deploy-image.bat          # Windows: deploy to AKS
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root:

```env
REDIS_HOST=your-cache.redis.cache.windows.net
REDIS_PORT=6380
REDIS_PASSWORD=your-redis-access-key
REDIS_SSL=true
GRPC_SERVICE_HOST=localhost
GRPC_SERVICE_PORT=50051
WEATHER_API_KEY=your-weather-api-key
```

### 2. Build and Start

```bash
# Build and start the application container
docker compose up --build

# Run in background
docker compose up --build -d
```

### 3. Access the Application

- **Application**: http://localhost:9080/modresorts
- **Health Check**: http://localhost:9080/modresorts/health

### 4. Stop the Application

```bash
docker compose down
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (defaults to `latest`)
2. Select registry type: **Azure ACR** or **Docker Hub**
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build

```bash
# Build the image
docker build -f Dockerfile -t modresorts:latest .

# Tag for ACR
docker tag modresorts:latest <ACR_NAME>.azurecr.io/modresorts:latest

# Login to ACR
az acr login --name <ACR_NAME>

# Push
docker push <ACR_NAME>.azurecr.io/modresorts:latest
```

---

## Azure AKS Deployment

### Step 1: Set Up Azure Resources

```bash
# Login to Azure
az login

# Create a resource group (if needed)
az group create --name modresorts-rg --location eastus

# Create an Azure Container Registry
az acr create --resource-group modresorts-rg --name <ACR_NAME> --sku Basic

# Create an AKS cluster
az aks create \
  --resource-group modresorts-rg \
  --name modresorts-aks \
  --node-count 2 \
  --node-vm-size Standard_DS2_v2 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Attach ACR to AKS (allows AKS to pull images without imagePullSecrets)
az aks update \
  --resource-group modresorts-rg \
  --name modresorts-aks \
  --attach-acr <ACR_NAME>
```

### Step 2: Install Application Gateway Ingress Controller (AGIC)

```bash
# Enable AGIC add-on on AKS
az aks enable-addons \
  --resource-group modresorts-rg \
  --name modresorts-aks \
  --addons ingress-appgw \
  --appgw-name modresorts-appgw \
  --appgw-subnet-cidr "10.225.0.0/16"
```

### Step 3: Build and Push the Image

```bash
./scripts/build-push.sh
# Select ACR, enter your ACR name, and tag (e.g., v2.0.0)
```

### Step 4: Deploy to AKS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- Azure Resource Group
- AKS Cluster name
- Full image URI (e.g., `myacr.azurecr.io/modresorts:v2.0.0`)
- Optional environment variables (Redis, gRPC, Weather API)

### Step 5: Verify Deployment

```bash
# Get credentials
az aks get-credentials --resource-group modresorts-rg --name modresorts-aks

# Check pods
kubectl get pods -n modresorts

# Check services and ingress
kubectl get svc,ingress -n modresorts

# View application logs
kubectl logs -f deployment/modresorts -n modresorts

# Test health endpoint
kubectl port-forward svc/modresorts-service 9080:80 -n modresorts
curl http://localhost:9080/modresorts/health
```

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `modresorts` namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (for high availability)
- **Image**: Placeholder `{{IMAGE_URI}}` replaced at deploy time
- **Resources**: requests `250m CPU / 512Mi RAM`, limits `500m CPU / 1Gi RAM`
- **Liveness Probe**: `GET /modresorts/health` on port 9080, starts after 60s
- **Readiness Probe**: `GET /modresorts/health` on port 9080, starts after 30s
- **Environment Variables**: All external service connections via env vars

### service.yaml
- **Type**: ClusterIP (internal only)
- **Port mapping**: `80 → 9080` (Liberty HTTP)

### ingress.yaml
- **Class**: `azure/application-gateway` (AGIC)
- **Host**: `modresorts.example.com` (update to your actual domain)
- **Path**: `/` → `modresorts-service:80`

---

## Configuration and Environment Variables

| Variable | Description | Default |
|---|---|---|
| `REDIS_HOST` | Azure Cache for Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port (6380 for Azure SSL) | `6380` |
| `REDIS_PASSWORD` | Redis access key | _(empty)_ |
| `REDIS_SSL` | Enable SSL for Redis | `true` |
| `GRPC_SERVICE_HOST` | gRPC service hostname | `localhost` |
| `GRPC_SERVICE_PORT` | gRPC service port | `50051` |
| `WEATHER_API_KEY` | External weather API key | _(empty)_ |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` |
| `TZ` | Container timezone | `UTC` |

### Using Azure Key Vault with AKS CSI Driver

For production, store secrets in Azure Key Vault and mount them via the CSI Secrets Store driver:

```bash
# Create Key Vault
az keyvault create --name modresorts-kv --resource-group modresorts-rg --location eastus

# Store secrets
az keyvault secret set --vault-name modresorts-kv --name redis-password --value "<your-redis-key>"
az keyvault secret set --vault-name modresorts-kv --name weather-api-key --value "<your-api-key>"
```

---

## Scaling and Management

### Manual Scaling

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

kubectl get hpa -n modresorts
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/modresorts \
  modresorts=<ACR_NAME>.azurecr.io/modresorts:v2.1.0 \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to specific revision
kubectl rollout history deployment/modresorts -n modresorts
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n modresorts

# Describe pod for events
kubectl describe pod <pod-name> -n modresorts

# Check logs
kubectl logs <pod-name> -n modresorts
kubectl logs <pod-name> -n modresorts --previous  # crashed container
```

### Health Check Failures

```bash
# Port-forward and test health endpoint
kubectl port-forward deployment/modresorts 9080:9080 -n modresorts
curl -v http://localhost:9080/modresorts/health
# Expected: HTTP 200 {"status":"UP","application":"ModResorts"}
```

### Image Pull Errors

```bash
# Verify ACR attachment
az aks check-acr --resource-group modresorts-rg --name modresorts-aks --acr <ACR_NAME>

# Check image exists in ACR
az acr repository show-tags --name <ACR_NAME> --repository modresorts
```

### Ingress Not Accessible

```bash
# Check ingress status
kubectl describe ingress modresorts-ingress -n modresorts

# Verify AGIC is running
kubectl get pods -n kube-system | grep ingress-appgw

# Check Application Gateway in Azure Portal
az network application-gateway show --resource-group modresorts-rg --name modresorts-appgw
```

### Redis Connection Issues

```bash
# Check environment variables in pod
kubectl exec -it deployment/modresorts -n modresorts -- env | grep REDIS

# Test Redis connectivity from pod
kubectl exec -it deployment/modresorts -n modresorts -- sh -c \
  "nc -zv $REDIS_HOST $REDIS_PORT && echo 'Redis reachable' || echo 'Redis unreachable'"
```

### OOMKilled (Out of Memory)

```bash
# Check resource usage
kubectl top pods -n modresorts

# Increase memory limits in deployment.yaml
# resources.limits.memory: "2Gi"
# Also increase JAVA_OPTS: -Xmx1g -Xms512m
```

---

## Security Considerations

1. **Non-root container**: The application runs as a non-root user (`appuser`) inside the container.
2. **No secrets in images**: All credentials are injected via environment variables or Azure Key Vault CSI.
3. **Network isolation**: Application is exposed only via ClusterIP service; external access is through the Application Gateway Ingress.
4. **Image scanning**: Enable Azure Defender for Containers to scan ACR images for vulnerabilities.
5. **RBAC**: Use Kubernetes RBAC to restrict access to the `modresorts` namespace.
6. **TLS**: Configure TLS termination at the Application Gateway level for HTTPS.
7. **Managed Identity**: Use Azure Workload Identity instead of service principal credentials for AKS-to-ACR authentication.

```bash
# Enable Azure Defender for Containers
az security pricing create --name Containers --tier Standard
```

---

## Java-Specific Notes

- **JVM Container Awareness**: `-XX:+UseContainerSupport` ensures the JVM respects container memory limits (Java 8u191+).
- **MaxRAMPercentage**: Set to `75.0` so the JVM heap uses at most 75% of the container memory limit.
- **Liberty Startup Time**: The liveness probe has a 60-second initial delay to allow Liberty to fully start before health checks begin.
- **WAR Context Root**: The application is deployed at `/modresorts` context root (WAR file name without version).
- **Java EE 7**: The application uses `javax.*` APIs (not `jakarta.*`). Ensure the Liberty server is configured with the appropriate Java EE 7 features.
- **CSS Optimization**: The Dockerfile includes a Node.js stage for PurgeCSS and cssnano optimization before the Java build stage.
