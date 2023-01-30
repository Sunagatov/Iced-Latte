#!/bin/bash
echo "-----------------------------------------------------------------------------------"
echo "########### Setting env variables ###########"
echo "-----------------------------------------------------------------------------------"
export PROJECT_NAME=online-store
export DOCKER_COMPOSE_YAML_FILE_PATH=./../../docker-compose.yaml
export DOCKER_COMPOSE_PROFILE=prod

echo "PROJECT_NAME                   = ${PROJECT_NAME}"
echo "DOCKER_COMPOSE_YAML_FILE_PATH  = ${DOCKER_COMPOSE_YAML_FILE_PATH}"
echo "DOCKER_COMPOSE_PROFILE         = ${DOCKER_COMPOSE_PROFILE}"

echo "-----------------------------------------------------------------------------------"
echo "########### Building docker compose images ###########"
echo "-----------------------------------------------------------------------------------"
docker-compose build
docker compose images

echo "-----------------------------------------------------------------------------------"
echo "########### Starting docker compose ###########"
echo "-----------------------------------------------------------------------------------"
docker-compose \
-f ${DOCKER_COMPOSE_YAML_FILE_PATH} \
-p ${PROJECT_NAME} \
--profile ${DOCKER_COMPOSE_PROFILE} up \
--remove-orphans