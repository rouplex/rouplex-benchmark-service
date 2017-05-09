#!/bin/bash
download_setup() {
    cd /home/ec2-user
    mkdir .ssh

    aws s3 cp s3://rouplex/deploys/access-keys/bitbucket-rouplex-deploy .ssh > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/access-keys/known_hosts .ssh > /dev/null 2>&1

    chmod 400 .ssh/bitbucket-rouplex-deploy
    chown -R ec2-user:ec2-user .ssh

    yum install -y git
    rm -rf rouplex-deploy-benchmark
    sudo -H -u ec2-user bash -c "ssh-agent bash -c 'ssh-add .ssh/bitbucket-rouplex-deploy; git clone ssh://bitbucket.org/rouplex/rouplex-deploy-benchmark.git --branch $1 --single-branch'"
}

run_setup() {
    sudo -H -u ec2-user bash -c "rouplex-deploy-benchmark/src/main/shell/common/ec2-setup.sh $1"
}

teardown_setup() {
    sudo -H -u ec2-user bash -c "rm -rf rouplex-deploy-benchmark"
}

download_setup master
run_setup default
teardown_setup
