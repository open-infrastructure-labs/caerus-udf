#!/bin/sh

export OPENFAAS_ADMIN_PASS=$(kubectl get secret -n openfaas basic-auth -o jsonpath="{.data.basic-auth-password}" | base64 --decode; echo)

echo "Secret is: $OPENFAAS_ADMIN_PASS"
