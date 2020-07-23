#!/usr/bin/env bash

source /var/run/secrets/nais.io/vault/credentials.env
export NODE_ENV=production

node build/server.js