#!/bin/bash
docker build -t $DOCKER_USERNAME/${IMAGE_NAME}:latest -t $DOCKER_USERNAME/${IMAGE_NAME}:$TRAVIS_BUILD_NUMBER -f ${DOCKER_FILE} .
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push $DOCKER_USERNAME/${IMAGE_NAME}:latest
docker push $DOCKER_USERNAME/${IMAGE_NAME}:$TRAVIS_BUILD_NUMBER