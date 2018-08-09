#!/bin/bash

# oneliner: the directory this script is in.
SRCDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


if [ "x-h" = "x$1" ] || [ "x--help" = "x$1" ] || [ "help" = "$1" ]; then
    echo "Usage: ./plutus-docker.sh {build|dev|check|run}"
    echo "./plutus-docker.sh build:  Builds the dockerfile, tagged \"plutus\""
    echo "                           All other commands build too."
    echo "./plutus-docker.sh check:  Build and run plutus --checks-only"
    echo "./plutus-docker.sh run:    Builds and starts plutus on port 32366"
    echo "./plutus-docker.sh dev:    Builds, then maps the source directory into docker and gives you a shell"
    exit -1
fi

docker build -t plutus . || exit 1

case "$1" in
  build)
    exit 0
    ;;
  check)
    docker run --rm -i -t --init plutus /build/plutus --checks-only || echo "FAIL: nonzero exit"
    ;;
  run)
    echo "Starting plutus exposed on port 32366"
    exec docker run --rm -i -t -p 32366:32366 --init plutus
    ;;
  dev)
    echo "Plutus docker-dev:"
    echo "You can edit sources, cd /build && ./plutus"
    exec docker run --rm -i -t -p 32366:32366 -v "$SRCDIR:/plutus:rw" plutus bash
    ;;
esac
