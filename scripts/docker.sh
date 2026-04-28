#!/usr/bin/env bash

set -e

COMMAND="$1"

case "$COMMAND" in
  dev)
    cd backend
    mvn package -DskipTests
    cd ..

    DOCKER_TARGET=dev docker compose up --build
    ;;

  prod)
    DOCKER_BUILDKIT=1 DOCKER_TARGET=prod docker compose up --build
    ;;

  down)
    docker compose down
    ;;

  clean)
    cd backend
    mvn clean
    cd ..

    docker compose down --rmi local --remove-orphans
    ;;

  *)
    echo "Command failed: ./scripts/docker.sh {dev|prod|down|clean}"
    exit 1
    ;;
esac
