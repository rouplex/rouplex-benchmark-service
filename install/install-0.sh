#!/bin/bash

# SYNOPSIS
#  install <service> <branch> <environment>
rouplex_install() {
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

    echo "=========== Rouplex ============= Installing git"
    sudo yum -y install git > /dev/null 2>&1

    echo "=========== Rouplex ============= Switching to ec2-user home"
    cd /home/ec2-user

    echo "=========== Rouplex ============= Downloading known_hosts from s3"
    aws s3 cp s3://rouplex/deploys/access-keys/known_hosts .ssh > /dev/null 2>&1

    echo "=========== Rouplex ============= Cloning service deployment scripts"
    git clone https://github.com/rouplex/"$1".git --branch "$2" --single-branch > /dev/null 2>&1

    echo "=========== Rouplex ============= Executing service deployment scripts"
    "$1"/install/install-1.sh "$3"

    echo "=========== Rouplex ============= Cleaning up"
    rm -rf "$1"
}

# branch (master or feature/some_feature), and environment (dev, prod, whatever configured at s3://rouplex/deploy ...)
rouplex_install rouplex-benchmark-service master prod
