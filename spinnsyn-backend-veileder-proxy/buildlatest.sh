echo "Bygger syfoapi latest for docker compose utvikling"

mvn clean install -D skipTests

docker build . -t spinnsyn-backend-veileder-proxy:latest
