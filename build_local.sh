#!/bin/bash
set -euxo pipefail

WORKDIR=$(pwd)

# Prerequisite:
#   server: Java 17
#   client: Node 18
#   generate_info_json.sh: jq
#   docker of course

# Get the start time
start_time=$(date +%s)

cd $WORKDIR/app/server
./build.sh -DskipTests

cd $WORKDIR/app/client

yarn && yarn build

cd $WORKDIR/app/client/packages/rts

yarn && yarn build

cd $WORKDIR
scripts/generate_info_json.sh

wait

echo "## Build docker image"
docker build --build-arg "BASE=appsmith/base-ce:release" -t bonitasoft/appsmith-ce:latest .

end_time=$(date +%s)
execution_time=$((end_time - start_time))

echo "Execution time: $execution_time seconds"
echo "All commands completed successfully"
