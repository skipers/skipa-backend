#!/bin/bash

set -euo pipefail

# =========================
# Config
# =========================

REGISTRY="amdp-registry.skala-ai.com"
PROJECT="skala3-finalproj-class2-team8"
IMAGE_NAME="skipa-backend"

NAMESPACE="skala3-finalproj-class2-team8"
DEPLOYMENT_NAME="skipa-backend"
CONTAINER_NAME="skipa-backend"

GIT_SHA=$(git rev-parse --short HEAD)
TAG="dev-${GIT_SHA}"

IMAGE="${REGISTRY}/${PROJECT}/${IMAGE_NAME}:${TAG}"
LATEST_IMAGE="${REGISTRY}/${PROJECT}/${IMAGE_NAME}:dev-latest"

# =========================
# Build Spring Boot jar
# =========================

echo "▶ Building Spring Boot jar..."
./gradlew clean bootJar

# =========================
# Docker buildx setup
# =========================

echo "▶ Checking Docker buildx builder..."

if ! docker buildx inspect skala-builder > /dev/null 2>&1; then
  docker buildx create --name skala-builder --use
else
  docker buildx use skala-builder
fi

# =========================
# Build and push image
# =========================

echo "▶ Building and pushing Docker image..."
echo "Image: ${IMAGE}"
echo "Latest: ${LATEST_IMAGE}"

docker buildx build \
  --platform linux/amd64 \
  -t "${IMAGE}" \
  -t "${LATEST_IMAGE}" \
  --push \
  .

# =========================
# Apply Kubernetes manifests
# =========================

echo "▶ Applying Kubernetes manifests..."

kubectl apply -f infra/k8s/backend-service.yml -n "${NAMESPACE}"
kubectl apply -f infra/k8s/backend-deployment.yml -n "${NAMESPACE}"

# =========================
# Update deployment image
# =========================

echo "▶ Updating deployment image..."
echo "Deployment: ${DEPLOYMENT_NAME}"
echo "Container: ${CONTAINER_NAME}"
echo "Image: ${IMAGE}"

kubectl set image deployment/"${DEPLOYMENT_NAME}" \
  "${CONTAINER_NAME}"="${IMAGE}" \
  -n "${NAMESPACE}"

# =========================
# Wait for rollout
# =========================

echo "▶ Waiting for rollout..."
kubectl rollout status deployment/"${DEPLOYMENT_NAME}" -n "${NAMESPACE}"

echo
echo "✅ Backend deployed successfully."
echo "Deployed image: ${IMAGE}"