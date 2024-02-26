# Prerequisite:
#   server: Java 17
#   client: Node 18
#   generate_info_json.sh: jq
#   docker of course
cd app/server
mvn clean compile -DskipTests

./build.sh -DskipTests

cd ../client

yarn install
yarn build

cd packages/rts

yarn install
yarn build

cd ../../../..

scripts/generate_info_json.sh

docker build --build-arg "BASE=appsmith/base-ce:release" -t appsmith/appsmith-ce:latest .
