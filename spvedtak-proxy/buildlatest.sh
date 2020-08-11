echo "Bygger base-proxy latest for docker compose utvikling"

npm i
npm run lint:fix
npm run build

docker build . -t base-proxy:latest
