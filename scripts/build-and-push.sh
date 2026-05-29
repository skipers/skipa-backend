#!/bin/bash

set -euo pipefail

# =========================
# Config
# =========================

REGISTRY="amdp-registry.skala-ai.com"
PROJECT="skala3-finalproj-class2-team8"
IMAGE_NAME="skipa-backend"

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

echo
echo "✅ Image pushed successfully."
echo
echo "Use this image for Kubernetes deployment:"
echo "${IMAGE}"
echo
echo "Or use latest dev image:"
echo "${LATEST_IMAGE}"