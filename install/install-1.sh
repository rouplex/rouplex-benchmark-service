#!/bin/bash

setup_tomcat_ssl_connector() {
    echo "=========== Rouplex ============= Setting up the tomcat's ssl connector"
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/server_key.p12 rouplex-environment > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/server_key.password rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/server-template.xml "#sslConnector-keystoreFile#" rouplex-environment/server_key.p12
    search_and_replace rouplex-benchmark-service/install/server-template.xml "#sslConnector-keystorePass#" `cat rouplex-environment/server_key.password`
    cp rouplex-benchmark-service/install/server-template.xml "$TOMCAT_FOLDER"/conf/server.xml
}

setup_tomcat_manager() {
    echo "=========== Rouplex ============= Setting up tomcat manager"
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/tomcat_manager.username rouplex-environment > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/tomcat_manager.password rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/tomcat-users-template.xml "#manager-username#" `cat rouplex-environment/tomcat_manager.username`
    search_and_replace rouplex-benchmark-service/install/tomcat-users-template.xml "#manager-password#" `cat rouplex-environment/tomcat_manager.password`
    cp rouplex-benchmark-service/install/tomcat-users-template.xml "$TOMCAT_FOLDER"/conf/tomcat-users.xml

    cp rouplex-benchmark-service/install/manager-context.xml "$TOMCAT_FOLDER"/webapps/manager/META-INF/context.xml
}

setup_tomcat_environment() {
    echo "=========== Rouplex ============= Setting up tomcat environment"
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/server_key.p12 rouplex-environment > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$environment"/server_key.password rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#keystoreFile#" `pwd`/rouplex-environment/server_key.p12
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#keystorePass#" `cat rouplex-environment/server_key.password`

    cp rouplex-benchmark-service/install/setenv-template.sh "$TOMCAT_FOLDER"/bin/setenv.sh
    chmod 500 "$TOMCAT_FOLDER"/bin/setenv.sh
}

setup_tomcat_initd() {
    echo "=========== Rouplex ============= Setting up tomcat as a service (initd)"
    search_and_replace rouplex-benchmark-service/install/tomcat-initd-template.sh "#CATALINA_HOME#" $TOMCAT_PATH
    search_and_replace rouplex-benchmark-service/install/tomcat-initd-template.sh "#CATALINA_BASE#" $TOMCAT_PATH

    sudo cp rouplex-benchmark-service/install/tomcat-initd-template /etc/init.d/tomcat
    sudo chmod 500 /etc/init.d/tomcat

    echo "=========== Rouplex ============= Configuring tomcat service to start on reboot"
    sudo chkconfig tomcat on
}

# SYNOPSIS
#  install <environment>
install() {
    git clone https://github.com/rouplex/rouplex-deploy.git --single-branch
    source rouplex-deploy/scripts/library.sh

    mkdir rouplex-environment

    install_jdk8
    install_tomcat "8.5.12" "catalina-jmx-remote.jar"

    setup_tomcat_ssl_connector
    setup_tomcat_manager
    setup_tomcat_environment
    setup_tomcat_initd
}

environment=prod
install
