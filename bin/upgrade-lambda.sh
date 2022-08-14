#!/bin/bash -e

SCRIPT_DIR=$(dirname ${BASH_SOURCE[0]})
PROJECT_HOME=$(cd ${SCRIPT_DIR}/..; pwd)

cd "${PROJECT_HOME}"
./gradlew clean buildLambda

cd terraform

terraform init
terraform apply
