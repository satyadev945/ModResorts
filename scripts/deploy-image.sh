#!/bin/bash
set -e
set -o pipefail

echo "============================================"
echo "  ModResorts - Deploy to Azure AKS"
echo "============================================"
echo ""

# ── Prompt for Azure / AKS details ──────────────────────────────────────────
read -p "Enter Azure Resource Group name: " RESOURCE_GROUP
if [ -z "$RESOURCE_GROUP" ]; then
  echo "ERROR: Resource group cannot be empty." >&2
  exit 1
fi

read -p "Enter AKS Cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: AKS cluster name cannot be empty." >&2
  exit 1
fi

read -p "Enter full Docker image URI (e.g. myacr.azurecr.io/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Image URI cannot be empty." >&2
  exit 1
fi

echo ""
echo "-- Optional: Application Environment Variables --"
echo "   Press Enter to skip any variable."
echo ""

read -p "Enter REDIS_HOST (Azure Cache for Redis hostname): " REDIS_HOST
read -p "Enter REDIS_PORT (default: 6380): " REDIS_PORT
read -s -p "Enter REDIS_PASSWORD (Azure Cache for Redis access key): " REDIS_PASSWORD
echo ""
read -p "Enter REDIS_SSL (true/false, default: true): " REDIS_SSL
read -p "Enter GRPC_SERVICE_HOST: " GRPC_SERVICE_HOST
read -p "Enter GRPC_SERVICE_PORT (default: 50051): " GRPC_SERVICE_PORT
read -s -p "Enter WEATHER_API_KEY: " WEATHER_API_KEY
echo ""

# Set defaults for optional vars
[ -z "$REDIS_PORT" ]        && REDIS_PORT="6380"
[ -z "$REDIS_SSL" ]         && REDIS_SSL="true"
[ -z "$GRPC_SERVICE_PORT" ] && GRPC_SERVICE_PORT="50051"

echo ""
echo "-- Configuring kubectl for AKS cluster: $CLUSTER_NAME --"
az aks get-credentials --resource-group "$RESOURCE_GROUP" --name "$CLUSTER_NAME" --overwrite-existing
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to get AKS credentials." >&2
  exit 1
fi

echo ""
echo "-- Verifying cluster connectivity --"
kubectl cluster-info || { echo "ERROR: Cannot connect to AKS cluster." >&2; exit 1; }

echo ""
echo "-- Updating Kubernetes manifests with deployment values --"

# Work on copies to avoid modifying originals
cp -r kubernetes kubernetes_deploy_tmp

sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g'             kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{REDIS_HOST}}|'"$REDIS_HOST"'|g'           kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{REDIS_PORT}}|'"$REDIS_PORT"'|g'           kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{REDIS_PASSWORD}}|'"$REDIS_PASSWORD"'|g'   kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{REDIS_SSL}}|'"$REDIS_SSL"'|g'             kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{GRPC_SERVICE_HOST}}|'"$GRPC_SERVICE_HOST"'|g' kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{GRPC_SERVICE_PORT}}|'"$GRPC_SERVICE_PORT"'|g' kubernetes_deploy_tmp/deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY"'|g' kubernetes_deploy_tmp/deployment.yaml

echo ""
echo "-- Applying Kubernetes manifests --"
kubectl apply -f kubernetes_deploy_tmp/namespace.yaml
echo "  [OK] Namespace applied"

kubectl apply -f kubernetes_deploy_tmp/deployment.yaml
echo "  [OK] Deployment applied"

kubectl apply -f kubernetes_deploy_tmp/service.yaml
echo "  [OK] Service applied"

kubectl apply -f kubernetes_deploy_tmp/ingress.yaml
echo "  [OK] Ingress applied"

# Clean up temporary copies
rm -rf kubernetes_deploy_tmp

echo ""
echo "-- Waiting for deployment rollout --"
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Running rollback ..." >&2
  kubectl rollout undo deployment/modresorts -n modresorts
  echo "Rollback initiated. Check pod status: kubectl get pods -n modresorts"
  exit 1
fi

echo ""
echo "-- Verifying deployed resources --"
kubectl get pods,svc,ingress -n modresorts

echo ""
echo "-- Application Access --"
INGRESS_IP=$(kubectl get ingress modresorts-ingress -n modresorts -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n modresorts -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "modresorts.example.com")
if [ -n "$INGRESS_IP" ]; then
  echo "  Application URL: http://$INGRESS_IP/modresorts"
else
  echo "  Application URL: http://$INGRESS_HOST/modresorts"
  echo "  (Update DNS to point $INGRESS_HOST to the ingress IP)"
fi

echo ""
echo "============================================"
echo "  Deployment completed successfully!"
echo "============================================"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n modresorts"
echo "  kubectl logs -f deployment/modresorts -n modresorts"
echo "  kubectl rollout undo deployment/modresorts -n modresorts  # rollback"
