#!/usr/bin/env bash
# =============================================================================
# build-push.sh – Build and push the ModResorts Docker image
# Supports: Google Artifact Registry | Docker Hub
# Usage   : bash scripts/build-push.sh   (run from repository root)
# =============================================================================
set -e
set -o pipefail

PROJECT_NAME="modresorts"

echo "=============================================="
echo "  ModResorts – Docker Build & Push"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Sanitise image name: lowercase, replace non-alphanumeric with hyphens,
# trim leading/trailing hyphens.
# ---------------------------------------------------------------------------
IMAGE_NAME=$(echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# ---------------------------------------------------------------------------
# Prompt for image tag
# ---------------------------------------------------------------------------
read -rp "Enter image tag [latest]: " IMAGE_TAG_INPUT
IMAGE_TAG=$(echo "${IMAGE_TAG_INPUT}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "${IMAGE_TAG}" ]; then
  IMAGE_TAG="latest"
fi
echo "Image tag: ${IMAGE_TAG}"
echo ""

# ---------------------------------------------------------------------------
# Registry selection
# ---------------------------------------------------------------------------
echo "Select container registry:"
echo "  1) Google Artifact Registry"
echo "  2) Docker Hub"
read -rp "Enter choice [1]: " REGISTRY_CHOICE
REGISTRY_CHOICE="${REGISTRY_CHOICE:-1}"

case "${REGISTRY_CHOICE}" in
  1)
    echo ""
    echo "--- Google Artifact Registry ---"
    read -rp "Enter GCP Project ID: " GCP_PROJECT
    read -rp "Enter GCP Region [us-central1]: " GCP_REGION
    GCP_REGION="${GCP_REGION:-us-central1}"
    read -rp "Enter Artifact Registry repository name [modresorts-repo]: " AR_REPO
    AR_REPO="${AR_REPO:-modresorts-repo}"

    FULL_IMAGE_NAME="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT}/${AR_REPO}/${IMAGE_NAME}:${IMAGE_TAG}"

    echo ""
    echo "Authenticating with Google Artifact Registry..."
    gcloud auth configure-docker "${GCP_REGION}-docker.pkg.dev" --quiet
    if [ $? -ne 0 ]; then
      echo "ERROR: Artifact Registry authentication failed." >&2
      exit 1
    fi
    ;;
  2)
    echo ""
    echo "--- Docker Hub ---"
    read -rp "Enter Docker Hub username: " DOCKER_USERNAME
    read -rsp "Enter Docker Hub password/token: " DOCKER_PASSWORD
    echo ""
    read -rp "Enter Docker Hub repository [${DOCKER_USERNAME}/${IMAGE_NAME}]: " DOCKER_REPO
    DOCKER_REPO="${DOCKER_REPO:-${DOCKER_USERNAME}/${IMAGE_NAME}}"

    FULL_IMAGE_NAME="${DOCKER_REPO}:${IMAGE_TAG}"

    echo ""
    echo "Authenticating with Docker Hub..."
    echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin
    if [ $? -ne 0 ]; then
      echo "ERROR: Docker Hub authentication failed." >&2
      exit 1
    fi
    ;;
  *)
    echo "ERROR: Invalid choice '${REGISTRY_CHOICE}'. Exiting." >&2
    exit 1
    ;;
esac

echo ""
echo "Full image name: ${FULL_IMAGE_NAME}"
echo ""

# ---------------------------------------------------------------------------
# Build the Docker image (build context is repository root)
# ---------------------------------------------------------------------------
echo "Building Docker image..."
docker build -f Dockerfile -t "${FULL_IMAGE_NAME}" .
if [ $? -ne 0 ]; then
  echo "ERROR: Docker build failed." >&2
  exit 1
fi
echo "Build successful."
echo ""

# ---------------------------------------------------------------------------
# Push the image
# ---------------------------------------------------------------------------
echo "Pushing image to registry..."
docker push "${FULL_IMAGE_NAME}"
if [ $? -ne 0 ]; then
  echo "ERROR: Docker push failed." >&2
  exit 1
fi

echo ""
echo "=============================================="
echo "  Image pushed successfully!"
echo "  ${FULL_IMAGE_NAME}"
echo "=============================================="
