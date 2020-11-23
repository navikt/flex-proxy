echo "Bygger flex-spring-proxy latest"

mvn clean install -D skipTests

docker build . -t flex-spring-proxy:latest
