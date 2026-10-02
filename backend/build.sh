#!/bin/bash

set -a
source "$(dirname "$0")/.env"
source "$(dirname "$0")/../version.env"
set +a

if [ -z "$IMAGE" ] || [ -z "$CONTAINER_CMD" ] || [ -z "$APP_VERSION" ]; then
    echo "IMAGE, CONTAINER_CMD or APP_VERSION is not set - check .env and ../version.env" >&2
    exit 1
fi

VER=$APP_VERSION
echo "*** Building $IMAGE version $VER ***"
rm -rf target
mvn clean install -Drevision=$VER
$CONTAINER_CMD build --build-arg VERSION=$VER -t $IMAGE:$VER .
