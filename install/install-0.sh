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

    echo "=========== Rouplex =============Installing git"
    sudo yum install git

    echo "=========== Rouplex =============Switching to ec2-user home"
    cd /home/ec2-user

    echo "=========== Rouplex =============Downloading known_hosts from s3"
    aws s3 cp s3://rouplex/deploys/access-keys/known_hosts ~/.ssh

    echo "=========== Rouplex =============Clonning service deployment scripts"
    git clone ssh://github.com/rouplex/"$1".git --branch "$2" --single-branch

    echo "=========== Rouplex =============Executing service deployment scripts"
    "$1"/install/install-1.sh "$3"
}

install benchmark-service "feature/distributed-testing" prod
