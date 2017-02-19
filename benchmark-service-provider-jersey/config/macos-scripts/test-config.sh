#!/usr/bin/env bash

if [[ $1 == "server" ]]
then
    echo "Rouplex --- Testing with server side authentication only"
    curl -X GET --header 'Accept: application/json' 'https://localhost:8088/rouplex/security/ping' --cacert ../sub-ca.crt
    if [[ $? != 0 ]]; then echo; echo "Rouplex --- Test failed"; exit 1; fi
elif [[ $1 == "mutual" ]]
then
    if [ -z "$2" ]; then echo; echo "Rouplex --- Test failed. Please specify the name of the client as second parameter ('meme' maybe?)"; exit 1; fi

    echo "Rouplex --- Testing with mutual authentication"
    curl -X GET --header 'Accept: application/json' 'https://localhost:8088/rouplex/security/ping' --cert ../client-keystore.p12:$2 --cacert ../sub-ca.crt
    if [[ $? != 0 ]]; then echo; echo "Rouplex --- Test failed"; exit 1; fi
else
    echo "Rouplex --- Test failed. Please specify first parameter from this set {server, mutual} depending on client auth you have on connector"
    exit 1
fi

echo; echo "Rouplex --- Test passed"
