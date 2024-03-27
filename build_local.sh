# Prerequisite:
#   server: Java 17
#   client: Node 18
#   generate_info_json.sh: jq
#   docker of course
cd app/server
./build.sh -DskipTests &

cd ../client

yarn && yarn build &

cd packages/rts

yarn && yarn build &

cd ../../../..

scripts/generate_info_json.sh

wait

echo "## Build docker image"
docker build --build-arg "BASE=appsmith/base-ce:release" -t bonitasoft/appsmith-ce:latest .
