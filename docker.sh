#!/bin/bash

DOCKERHUB_USERNAME="zufarexplainedit"
DOCKERHUB_REPO="online-store"
IMAGE_NAME="$DOCKERHUB_USERNAME/$DOCKERHUB_REPO"
TAG="latest"
DOCKERFILE_PATH="./Dockerfile"

echo "Building Docker image..."
docker build -t $IMAGE_NAME:$TAG -f $DOCKERFILE_PATH .

echo "Tagging Docker image..."
docker tag $IMAGE_NAME:$TAG $DOCKERHUB_USERNAME/$DOCKERHUB_REPO:$TAG

echo "Logging into Docker..."
docker login --username DOCKERHUB_USERNAME
echo "Logging into Docker is successful"

echo "Pushing Docker image to Docker Hub..."
docker push $DOCKERHUB_USERNAME/$DOCKERHUB_REPO:$TAG

echo "Done."
