# ModResorts – Deployment Guide (AWS EKS)

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build & Push Docker Image](#build--push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment Walkthrough](#kubernetes-deployment-walkthrough)
8. [Configuration Management](#configuration-management)
9. [Scaling & Management](#scaling--management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

**ModResorts** is a Java EE 7 web application packaged as a WAR and deployed on **Open Liberty**. It provides resort availability checking, weather information, and customer management features.

| Property | Value |
|---|---|
| Artifact | `modresorts-2.0.0.war` |
| Build tool | Maven 3.9.x |
| Java version | 8 (runtime: `amazoncorretto:8`) |
| Application server | Open Liberty 23.0.0.12 |
| HTTP port | 9080 |
| HTTPS port | 9443 |
| Health endpoint | `GET /modresorts/health` |
| Target platform | AWS EKS |

---

## Prerequisites

### Local Machine
- Docker Desktop 24+ or Docker Engine 24+
- Docker Compose v2+
- AWS CLI v2 (`aws --version`)
- `kubectl` 1.28+ (`kubectl version --client`)
- `eksctl` 0.160+ (optional, for cluster creation)
- Git

### AWS Account
- IAM user/role with permissions: `eks:*`, `ecr:*`, `ec2:*`, `iam:PassRole`
- AWS CLI configured: `aws configure`

---

## Local Development with Docker Compose

### 1. Set environment variables

Create a `.env` file in the project root:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
WEATHER_API_KEY=your_api_key_here
WEATHER_SERVICE_URL=
```

### 2. Start the application

```bash
docker compose up --build
```

### 3. Access the application

- Application: http://localhost:9080/modresorts
- Health check: http://localhost:9080/modresorts/health

### 4. Stop the application

```bash
docker compose down
```

---

## Build & Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Select registry (AWS ECR or Docker Hub)
2. Enter registry credentials / AWS account details
3. Enter an image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual build (ECR example)

```bash
AWS_ACCOUNT_ID=123456789012
AWS_REGION=us-east-1
IMAGE_TAG=2.0.0

aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

aws ecr create-repository --repository-name modresorts --region $AWS_REGION 2>/dev/null || true

docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/modresorts:${IMAGE_TAG} .
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/modresorts:${IMAGE_TAG}
```

---

## AWS EKS Prerequisites

### Install required tools

```bash
# AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# eksctl (optional)
curl --silent --location "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

### Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

---

## EKS Cluster Setup

### Option A – Use an existing cluster

```bash
aws eks update-kubeconfig --region us-east-1 --name my-cluster
kubectl cluster-info
```

### Option B – Create a new cluster with eksctl

```bash
eksctl create cluster \
  --name modresorts-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

### Install AWS Load Balancer Controller (required for Ingress)

```bash
# Add IAM policy
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.1/docs/install/iam_policy.json
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# Install via Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=modresorts-cluster \
  --set serviceAccount.create=true
```

---

## Kubernetes Deployment Walkthrough

### Manifest descriptions

| File | Kind | Purpose |
|---|---|---|
| `kubernetes/namespace.yaml` | Namespace | Isolates ModResorts resources in the `modresorts` namespace |
| `kubernetes/deployment.yaml` | Deployment | Runs 2 replicas of the ModResorts container |
| `kubernetes/service.yaml` | Service (ClusterIP) | Internal load balancer; exposes port 80→9080 |
| `kubernetes/ingress.yaml` | Ingress (ALB) | Internet-facing AWS ALB; routes traffic to the service |

### Deploy using the script

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- AWS region and EKS cluster name
- Full Docker image URI
- Optional: REDIS_HOST, REDIS_PORT, WEATHER_API_KEY, WEATHER_SERVICE_URL

### Manual deployment

```bash
# 1. Update the image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:2.0.0|g' kubernetes/deployment.yaml

# 2. Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 3. Wait for rollout
kubectl rollout status deployment/modresorts -n modresorts

# 4. Verify
kubectl get pods,svc,ingress -n modresorts
```

---

## Configuration Management

### Environment variables

| Variable | Required | Description |
|---|---|---|
| `REDIS_HOST` | Yes (production) | Amazon ElastiCache primary endpoint |
| `REDIS_PORT` | No | Redis port (default: 6379) |
| `WEATHER_API_KEY` | No | Wunderground API key for live weather data |
| `WEATHER_SERVICE_URL` | No | Override weather service base URL |
| `JVM_ARGS` | No | JVM flags (default: `-Xmx512m -Xms256m`) |
| `TZ` | No | Timezone (default: UTC) |

### Using Kubernetes Secrets for sensitive values

```bash
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your_api_key \
  -n modresorts
```

Then reference in `deployment.yaml`:
```yaml
- name: WEATHER_API_KEY
  valueFrom:
    secretKeyRef:
      name: modresorts-secrets
      key: WEATHER_API_KEY
```

---

## Scaling & Management

### Manual scaling

```bash
kubectl scale deployment modresorts --replicas=4 -n modresorts
```

### Horizontal Pod Autoscaler

```bash
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling update

```bash
kubectl set image deployment/modresorts \
  modresorts=123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:2.1.0 \
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

### Pods not starting

```bash
kubectl describe pod -l app=modresorts -n modresorts
kubectl logs -l app=modresorts -n modresorts --previous
```

### Health check failures

```bash
# Check health endpoint directly from inside a pod
kubectl exec -it $(kubectl get pod -l app=modresorts -n modresorts -o jsonpath='{.items[0].metadata.name}') \
  -n modresorts -- wget -qO- http://localhost:9080/modresorts/health
```

### Ingress / ALB not provisioning

```bash
kubectl describe ingress modresorts-ingress -n modresorts
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
```

### Redis connection issues

```bash
# Verify REDIS_HOST env var is set correctly
kubectl exec -it <pod-name> -n modresorts -- env | grep REDIS
```

### Image pull errors

```bash
# Verify ECR permissions
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Check node IAM role has AmazonEC2ContainerRegistryReadOnly policy
```

---

## Security Considerations

1. **Non-root container**: The application runs as the `liberty` user (non-root).
2. **Secrets management**: Store `WEATHER_API_KEY` and database credentials in Kubernetes Secrets, not ConfigMaps.
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod traffic.
4. **Image scanning**: Enable ECR image scanning on push: `aws ecr put-image-scanning-configuration --repository-name modresorts --image-scanning-configuration scanOnPush=true`.
5. **TLS**: The ALB Ingress is configured to redirect HTTP→HTTPS. Attach an ACM certificate via the `alb.ingress.kubernetes.io/certificate-arn` annotation.
6. **RBAC**: Use least-privilege IAM roles for EKS node groups and service accounts.

---

## Technology-Specific Notes

### Open Liberty on Amazon Corretto 8

- The runtime image is `amazoncorretto:8` with Open Liberty 23.0.0.12 installed.
- Liberty features are installed at image build time via `installUtility install`.
- The server configuration lives in `src/main/liberty/config/` and is copied into the image.
- JVM flags are set via the `JVM_ARGS` environment variable.

### JVM Container Awareness

The following flags are set by default:
```
-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```
`UseContainerSupport` ensures the JVM respects cgroup memory limits rather than reading host memory.

### WAR Context Root

The application is deployed at context root `/modresorts`. All URLs are prefixed accordingly:
- Application: `http://<host>/modresorts/`
- Health: `http://<host>/modresorts/health`

### CSS Optimisation (Multi-stage Build)

The Dockerfile includes a Node.js CSS build stage that:
1. Runs **PurgeCSS** to remove unused CSS selectors (reduces image size).
2. Runs **cssnano** to minify the remaining CSS.

The optimised CSS is embedded in the WAR before the final image is assembled.
