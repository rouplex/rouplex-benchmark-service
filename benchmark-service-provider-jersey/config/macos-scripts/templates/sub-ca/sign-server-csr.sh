#!/usr/bin/env bash

if [ -z "$1" ]
then
    echo "Please supply the server csr file (input -- certificate to sign)"
    exit 1
fi

if [ -z "$2" ]
then
    echo "Please supply the server crt file (output -- signed certificate)"
    exit 1
fi

openssl ca -config sub-ca.conf -in $1 -out $2 -extensions server_ext
