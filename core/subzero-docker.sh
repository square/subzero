#!/bin/bash

# oneliner: the directory this script is in.
SRCDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


if [ "x-h" = "x$1" ] || [ "x--help" = "x$1" ] || [ "help" = "$1" ]; then
    echo "Usage: ./subzero-docker.sh {build|dev|check|run}"
    echo "./subzero-docker.sh build:  Builds the dockerfile, tagged \"subzero\""
    echo "                           All other commands build too."
    echo "./subzero-docker.sh check:  Build and run subzero --checks-only"
    echo "./subzero-docker.sh run:    Builds and starts subzero on port 32366"
    echo "./subzero-docker.sh dev:    Builds, then maps the source directory into docker and gives you a shell"
    exit -1
fi

docker build -t subzero . || exit 1

case "$1" in
  build)
    exit 0
    ;;
  check)
    docker run --rm -i -t --init subzero /build/subzero --checks-only || echo "FAIL: nonzero exit"
    ;;
  run)
    echo "Starting subzero exposed on port 32366"
    exec docker run --rm -i -t -p 32366:32366 --init subzero
    ;;
  dev)
    echo "Plutus docker-dev:"
    echo "You can edit sources, cd /build && ./subzero"
    exec docker run --rm -i -t -p 32366:32366 -v "$SRCDIR:/subzero:rw" subzero bash
    ;;
esac
