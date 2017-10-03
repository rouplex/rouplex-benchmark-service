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
    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/server_port.txt rouplex-environment > /dev/null 2>&1
    local server_port=`cat rouplex-environment/server_port.txt`
    if (( server_port < 1025 )); then
        rpm -Uvh https://s3.amazonaws.com/aaronsilber/public/authbind-2.1.1-0.1.x86_64.rpm
        touch /etc/authbind/byport/$server_port
        chmod 500 /etc/authbind/byport/$server_port
        chown ec2-user /etc/authbind/byport/$server_port
        search_and_replace $TOMCAT_PATH/bin/startup.sh "exec " "exec authbind --deep "
    fi

    search_and_replace rouplex-benchmark-service/install/server-template.xml "#sslConnector-sslPort#" $server_port
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

    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/benchmark_main_url.txt rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#BenchmarkMainUrl#" `cat rouplex-environment/benchmark_main_url.txt`

    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/google_cloud.clientid rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#GoogleCloudClientId#" `cat rouplex-environment/google_cloud.clientid`

    aws s3 cp s3://rouplex/deploys/services/benchmark/environments/"$ENV"/google_cloud.password rouplex-environment > /dev/null 2>&1
    search_and_replace rouplex-benchmark-service/install/setenv-template.sh "#GoogleCloudClientPassword#" `cat rouplex-environment/google_cloud.password`

    cp rouplex-benchmark-service/install/setenv-template.sh "$TOMCAT_FOLDER"/bin/setenv.sh
    chmod 700 "$TOMCAT_FOLDER"/bin/setenv.sh

    cp rouplex-benchmark-service/install/logging-properties "$TOMCAT_FOLDER"/conf/logging-properties
}

setup_tomcat_initd() {
    echo "=========== Rouplex ============= Setting up tomcat as a service (initd)"
    search_and_replace rouplex-benchmark-service/install/tomcat-initd-template "#CATALINA_HOME#" $TOMCAT_PATH
    search_and_replace rouplex-benchmark-service/install/tomcat-initd-template "#CATALINA_BASE#" $TOMCAT_PATH

    sudo cp rouplex-benchmark-service/install/tomcat-initd-template /etc/init.d/tomcat
    sudo chmod 500 /etc/init.d/tomcat

    echo "=========== Rouplex ============= Configuring tomcat service to start on reboot"
    sudo chkconfig tomcat on
}

start_tomcat() {
    echo "=========== Rouplex ============= Changing tomcat ownership to ec2-user"
    chown -R ec2-user:ec2-user $TOMCAT_PATH

    echo "=========== Rouplex ============= Starting tomcat service"
    service tomcat restart
}

install_tools() {
    echo "=========== Rouplex ============= Installing sysstat"
    yum -y install sysstat

    echo "=========== Rouplex ============= Installing htop"
    yum -y install htop

    # echo "=========== Rouplex ============= Installing jhiccup"
    # curl -o jHiccup http://www.azul.com/files/jHiccup-2.0.7-dist.tar.gz
    # ...
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
    install_tools

    setup_tomcat_ssl_certificate
    setup_tomcat_ssl_connector
    setup_tomcat_manager
    setup_tomcat_run_environment
    setup_tomcat_initd
    start_tomcat

    echo "=========== Rouplex ============= Cleaning up"
    rm -rf rouplex-environment
    rm -rf rouplex-deploy
}

install $1
