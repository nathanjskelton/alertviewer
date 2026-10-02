#!/bin/bash

set -a
source "$(dirname "$0")/.env"
source "$(dirname "$0")/../version.env"
set +a

if [ -z "$IMAGE" ] || [ -z "$CONTAINER_CMD" ] || [ -z "$APP_VERSION" ]; then
    echo "IMAGE, CONTAINER_CMD or APP_VERSION is not set - check .env and ../version.env" >&2
    exit 1
fi

X=$APP_VERSION

echo "*** Building $IMAGE version $X ***"
npm run build
$CONTAINER_CMD build --build-arg APP_VERSION=$X -t $IMAGE:$X .
