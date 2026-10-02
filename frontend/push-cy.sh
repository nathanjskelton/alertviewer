#!/bin/bash

set -a
source "$(dirname "$0")/.env"
set +a

if [ -z "$IMAGE" ] || [ -z "$CONTAINER_CMD" ]; then
    echo "IMAGE or CONTAINER_CMD is not set - check .env" >&2
    exit 1
fi

X=`node get_version.js`
echo "*** Pushing $IMAGE version $X ***"
$CONTAINER_CMD push $IMAGE:$X
