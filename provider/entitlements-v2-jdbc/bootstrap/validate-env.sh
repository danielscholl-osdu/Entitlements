#!/usr/bin/env bash

{ set +x ;} 2> /dev/null # disable output to prevent secret logging
set -e

ENV_VAR_NAME=$1

if [ "${!ENV_VAR_NAME}" = "" ]
then
    echo "Missing environment variable '$ENV_VAR_NAME'. Please provide all variables and try again"
    { set -x ;} 2> /dev/null # enable output back
    exit 1
fi

{ set -x ;} 2> /dev/null # enable output back
