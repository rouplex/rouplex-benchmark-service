#!/bin/bash
download_setup() {
    cd /home/ec2-user
    mkdir .ssh

    aws s3 cp s3://rouplex/deploys/access-keys/bb-rouplex-deploy.pem .ssh > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/access-keys/known_hosts .ssh > /dev/null 2>&1

    chmod 400 .ssh/bb-rouplex-deploy.pem
    chown -R ec2-user:ec2-user .ssh

    yum install -y git
    rm -rf rouplex-deploy-benchmark
    sudo -H -u ec2-user bash -c "ssh-agent bash -c 'ssh-add .ssh/bb-rouplex-deploy.pem; git clone ssh://bitbucket.org/rouplex/rouplex-deploy-benchmark.git --branch $1 --single-branch'"
}

download_setup master
sudo -H -u ec2-user bash -c rouplex-deploy-benchmark/src/main/shell/common/ec2-setup.sh default
