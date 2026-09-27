#!/bin/bash
set -e
set -o pipefail

APP_NAME="modresorts"
NAMESPACE="modresorts"

echo "============================================"
echo "  ModResorts – AWS EKS Deployment Script"
echo "============================================"
echo ""

# ── Collect required inputs ──────────────────────────────────────────────────
read -rp "Enter AWS region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS region is required." && exit 1
fi

read -rp "Enter EKS cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS cluster name is required." && exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required." && exit 1
fi

echo ""
echo "── Optional environment variable configuration ──────────────────────────"
echo "Press Enter to skip any variable (placeholder will remain in manifest)."
echo ""

read -rp "Enter REDIS_HOST value (ElastiCache endpoint, or Enter to skip): " REDIS_HOST_VAL
read -rp "Enter REDIS_PORT value (default 6379, or Enter to skip): " REDIS_PORT_VAL
read -rp "Enter WEATHER_API_KEY value (or Enter to skip): " WEATHER_API_KEY_VAL
read -rp "Enter WEATHER_SERVICE_URL value (or Enter to skip): " WEATHER_SERVICE_URL_VAL

echo ""
echo "── Configuring kubectl for EKS cluster ─────────────────────────────────"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster."; exit 1; }

echo ""
echo "── Updating Kubernetes manifests ───────────────────────────────────────"

# Replace IMAGE_URI placeholder
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g" kubernetes/deployment.yaml

# Replace optional env var placeholders if values were provided
if [ -n "$REDIS_HOST_VAL" ]; then
  sed -i "s|{{REDIS_HOST}}|${REDIS_HOST_VAL}|g" kubernetes/deployment.yaml
fi
if [ -n "$REDIS_PORT_VAL" ]; then
  sed -i "s|{{REDIS_PORT}}|${REDIS_PORT_VAL}|g" kubernetes/deployment.yaml
fi
if [ -n "$WEATHER_API_KEY_VAL" ]; then
  sed -i "s|{{WEATHER_API_KEY}}|${WEATHER_API_KEY_VAL}|g" kubernetes/deployment.yaml
fi
if [ -n "$WEATHER_SERVICE_URL_VAL" ]; then
  sed -i "s|{{WEATHER_SERVICE_URL}}|${WEATHER_SERVICE_URL_VAL}|g" kubernetes/deployment.yaml
fi

echo ""
echo "── Applying Kubernetes manifests ───────────────────────────────────────"
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "── Waiting for deployment rollout ──────────────────────────────────────"
kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s

echo ""
echo "── Verifying deployed resources ────────────────────────────────────────"
kubectl get pods,svc,ingress -n ${NAMESPACE}

echo ""
echo "── Application access URL ──────────────────────────────────────────────"
INGRESS_HOST=$(kubectl get ingress ${APP_NAME}-ingress -n ${NAMESPACE} \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "<pending>")
echo "  http://${INGRESS_HOST}/modresorts"

echo ""
echo "============================================"
echo "  Deployment complete!"
echo "============================================"
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}"
