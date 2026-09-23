#!/usr/bin/env bash
# =============================================================================
# deploy-image.sh – Deploy ModResorts to GCP GKE
# Usage: bash scripts/deploy-image.sh   (run from repository root)
# =============================================================================
set -e
set -o pipefail

APP_NAME="modresorts"
NAMESPACE="modresorts"

echo "=============================================="
echo "  ModResorts – GKE Deployment"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Collect GCP / GKE parameters
# ---------------------------------------------------------------------------
read -rp "Enter GCP Project ID: " GCP_PROJECT
if [ -z "${GCP_PROJECT}" ]; then
  echo "ERROR: GCP Project ID is required." >&2
  exit 1
fi

read -rp "Enter GCP Zone [us-central1-a]: " GCP_ZONE
GCP_ZONE="${GCP_ZONE:-us-central1-a}"

read -rp "Enter GKE Cluster Name [modresorts-autopilot]: " CLUSTER_NAME
CLUSTER_NAME="${CLUSTER_NAME:-modresorts-autopilot}"

read -rp "Enter full Docker image URI (e.g. us-central1-docker.pkg.dev/my-project/repo/modresorts:latest): " IMAGE_URI
if [ -z "${IMAGE_URI}" ]; then
  echo "ERROR: Docker image URI is required." >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Optional application environment variables
# ---------------------------------------------------------------------------
echo ""
echo "--- Optional Application Configuration ---"
echo "(Press Enter to skip any value)"

read -rp "Enter REDIS_HOST (Google Cloud Memorystore host): " REDIS_HOST_VAL
REDIS_HOST_VAL="${REDIS_HOST_VAL:-}"

read -rp "Enter REDIS_PORT [6379]: " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"

read -rp "Enter WUNDERGROUND_API_KEY: " WUNDERGROUND_API_KEY_VAL
WUNDERGROUND_API_KEY_VAL="${WUNDERGROUND_API_KEY_VAL:-}"

# ---------------------------------------------------------------------------
# Configure kubectl for the target GKE cluster
# ---------------------------------------------------------------------------
echo ""
echo "Configuring kubectl for cluster '${CLUSTER_NAME}'..."
gcloud container clusters get-credentials "${CLUSTER_NAME}" \
  --zone "${GCP_ZONE}" \
  --project "${GCP_PROJECT}"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster." >&2; exit 1; }

# ---------------------------------------------------------------------------
# Substitute placeholders in Kubernetes manifests
# ---------------------------------------------------------------------------
echo ""
echo "Updating Kubernetes manifests with deployment values..."

# Work on copies to avoid modifying the originals permanently
cp kubernetes/deployment.yaml /tmp/deployment.yaml

sed -i 's|{{IMAGE_URI}}|'"${IMAGE_URI}"'|g'              /tmp/deployment.yaml
sed -i 's|{{REDIS_HOST}}|'"${REDIS_HOST_VAL}"'|g'        /tmp/deployment.yaml
sed -i 's|{{REDIS_PORT}}|'"${REDIS_PORT_VAL}"'|g'        /tmp/deployment.yaml
sed -i 's|{{WUNDERGROUND_API_KEY}}|'"${WUNDERGROUND_API_KEY_VAL}"'|g' /tmp/deployment.yaml

# ---------------------------------------------------------------------------
# Apply manifests in order
# ---------------------------------------------------------------------------
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f /tmp/deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f kubernetes/service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

# ---------------------------------------------------------------------------
# Wait for rollout
# ---------------------------------------------------------------------------
echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Running rollback..." >&2
  kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}
  exit 1
fi

# ---------------------------------------------------------------------------
# Verify resources
# ---------------------------------------------------------------------------
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n ${NAMESPACE}

# ---------------------------------------------------------------------------
# Display access URL
# ---------------------------------------------------------------------------
echo ""
echo "Fetching application URL from Ingress..."
INGRESS_IP=$(kubectl get ingress modresorts-ingress -n ${NAMESPACE} \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")

if [ "${INGRESS_IP}" = "pending" ] || [ -z "${INGRESS_IP}" ]; then
  echo "Ingress IP is still being provisioned. Check later with:"
  echo "  kubectl get ingress modresorts-ingress -n ${NAMESPACE}"
else
  echo "Application is accessible at: http://${INGRESS_IP}"
fi

echo ""
echo "=============================================="
echo "  Deployment complete!"
echo "  Namespace : ${NAMESPACE}"
echo "  Image     : ${IMAGE_URI}"
echo "=============================================="
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n ${NAMESPACE}"
echo "  kubectl logs -f deployment/${APP_NAME} -n ${NAMESPACE}"
echo "  kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}  # rollback"
