#!/bin/bash

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

for project in alertviewer-backend alertviewer-ui; do
    echo ""
    echo "=== $project: build ==="
    (cd "$ROOT/$project" && ./build.sh)

    echo ""
    echo "=== $project: push ==="
    (cd "$ROOT/$project" && ./push-cy.sh)
done

echo ""
echo "=== All projects built and pushed ==="
