#!/bin/bash

# SYNOPSIS
#  install <service> <branch> <environment>
install() {
    if [ "$#" -lt 1 ]; then
        echo "=========== Rouplex ============= Exiting. Missing <service> param in install()"
        exit 1
    fi

    if [ "$#" -lt 2 ]; then
        echo "=========== Rouplex ============= Exiting. Missing <branch> param in install()"
        exit 1
    fi

    if [ "$#" -lt 3 ]; then
        echo "=========== Rouplex ============= Exiting. Missing <environment> param in install()"
        exit 1
    fi

    cd /home/ec2-user
    git clone https://github.com/rouplex/"$1".git --branch "$2" --single-branch
    "$1"/install/install-1.sh "$3"
}

install benchmark-service "feature/distributed-testing" prod
