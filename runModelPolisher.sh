#!/bin/bash

# Check if Docker is installed
if [ ! -x "$(command -v docker)" ]; then
    echo "Docker is not installed, please set up Docker"
    exit -1
fi

# Check if the path to the models folder is provided
MODELS=$1
if [ -z "${MODELS}" ]; then
    echo "Path to model folder is either empty or not set. Please set the correct path"
    exit -2
fi

# Check if the provided path is a directory
if [ ! -d "${MODELS}" ]; then
    echo "Path does not point to a directory: ${MODELS}"
    exit -3
fi

# Run the Docker container with the provided models path
docker-compose run \
    -u $(id -u):$(id -g) \
    -v ${MODELS}:/models \
    polisher --input=/models --output=/models/out --annotate-with-bigg=true --sbml-validation=true --compression-type=GZIP
