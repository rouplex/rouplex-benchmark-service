#!/bin/bash
download_setup() {
    sudo yum install -y git
    mkdir .ssh
    aws s3 cp s3://rouplex/deploys/access-keys/bb-rouplex-deploy.pem .ssh > /dev/null 2>&1
    chmod 400 .ssh/bb-rouplex-deploy.pem
    aws s3 cp s3://rouplex/deploys/access-keys/known_hosts .ssh > /dev/null 2>&1
    rm -rf rouplex-deploy-benchmark
    ssh-agent bash -c "ssh-add .ssh/bb-rouplex-deploy.pem; git clone ssh://bitbucket.org/rouplex/rouplex-deploy-benchmark.git --branch $1 --single-branch" > /dev/null 2>&1
}

download_setup master
rouplex-deploy-benchmark/src/main/shell/common/ec2-setup.sh default
