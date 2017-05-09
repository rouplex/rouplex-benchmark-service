#!/bin/bash

setup_tomcat_ssl_certificate() {
    echo "=========== Rouplex ============= Setting up the tomcat's ssl certificate"
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/server_key.p12 $TOMCAT_PATH/conf > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/server_key.password rouplex-environment > /dev/null 2>&1

    TOMCAT_CERT_PATH=$TOMCAT_PATH/conf/server_key.p12
    TOMCAT_CERT_PASS=`cat rouplex-environment/server_key.password`
}

setup_tomcat_ssl_connector() {
    echo "=========== Rouplex ============= Setting up the tomcat's ssl connector"
    search_and_replace rouplex-benchmark-service/install/server-template.xml "#sslConnector-keystoreFile#" $TOMCAT_CERT_PATH
    search_and_replace rouplex-benchmark-service/install/server-template.xml "#sslConnector-keystorePass#" $TOMCAT_CERT_PASS
    cp rouplex-benchmark-service/install/server-template.xml "$TOMCAT_FOLDER"/conf/server.xml
}

setup_tomcat_manager() {
    echo "=========== Rouplex ============= Setting up tomcat manager"
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/tomcat_manager.username rouplex-environment > /dev/null 2>&1
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/tomcat_manager.password rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/tomcat-users-template.xml "#manager-username#" `cat rouplex-environment/tomcat_manager.username`
    search_and_replace rouplex-benchmark-service/install/tomcat-users-template.xml "#manager-password#" `cat rouplex-environment/tomcat_manager.password`
    cp rouplex-benchmark-service/install/tomcat-users-template.xml "$TOMCAT_FOLDER"/conf/tomcat-users.xml

    cp rouplex-benchmark-service/install/manager-context.xml "$TOMCAT_FOLDER"/webapps/manager/META-INF/context.xml
}

setup_tomcat_run_environment() {
    echo "=========== Rouplex ============= Setting up tomcat environment"
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#keystoreFile#" $TOMCAT_CERT_PATH
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#keystorePass#" $TOMCAT_CERT_PASS

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
    if [ "$#" -lt 1 ]; then
        echo "=========== Rouplex ============= Exiting. Missing <environment> param in install()"
        exit 1
    fi

    ENV=$1

    git clone https://github.com/rouplex/rouplex-deploy.git --single-branch
    source rouplex-deploy/scripts/library.sh

    mkdir rouplex-environment

    install_jdk8
    install_tomcat "8.5.12" "catalina-jmx-remote.jar"

    setup_tomcat_ssl_certificate
    setup_tomcat_ssl_connector
    setup_tomcat_manager
    setup_tomcat_run_environment
    setup_tomcat_initd
}

install $1
