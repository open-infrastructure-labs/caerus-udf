#!/bin/sh

export OPENFAAS_PASSWORD=$(kubectl get secret -n openfaas basic-auth -o jsonpath="{.data.basic-auth-password}" | base64 --decode; echo)
echo "Secret is: $OPENFAAS_PASSWORD"
export CAERUS_REDIS_HOSTNAME=caerus-redis-primary
export OPENFAAS_USERNAME=admin
export OPENFAAS_BASEPATH=http://127.0.0.1:8080
export MINIO_URL=http://localhost:9000