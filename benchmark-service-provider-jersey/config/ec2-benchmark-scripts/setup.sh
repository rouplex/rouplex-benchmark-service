#!/bin/bash

init_setup() {
  if [ `uname` != "Linux" ]; then
    echo "=========== Rouplex ============= Aborting setup. Cause: Only Linux is supported by this setup"
    exit 1
  fi

  if [ `uname -m` != "x86_64" ]; then
    echo "=========== Rouplex ============= Aborting setup. Cause: Only 64 bit architectures supported by this setup"
    exit 1
  fi

  if [ $# -lt 2 ]; then
    echo "=========== Rouplex ============= Aborting setup. Cause: No GIT_BRANCH / DOMAIN_NAME defined"
    exit 1
  else
    GIT_BRANCH=$1
    SERVER_KEYSTORE=$2.p12
  fi

  cd /home/ec2-user
  JDK8_NAME=jdk1.8.0_121.x86_64
  JDK8_RPM="jdk-8u121-linux-x64.rpm"
  TOMCAT8="apache-tomcat-8.5.12"
  TOMCAT8_GZ=$TOMCAT8.tar.gz
  HOST_NAME="rouplex-demo.com"
  GITHUB_DEPLOY_FOLDER="https://raw.githubusercontent.com/rouplex/rouplex-benchmark-service/$GIT_BRANCH/benchmark-service-provider-jersey/config/ec2-benchmark-scripts/templates"
  S3_DEPLOY_FOLDER=s3://rouplex/deploys/${GIT_BRANCH}
  S3_DEFAULT_DEPLOY_FOLDER=s3://rouplex/deploys/defaults
}

setup_java() {
  if yum list installed $JDK8_NAME >/dev/null 2>&1; then
    echo "=========== Rouplex ============= Skipping install of rpm $JDK8_NAME (already installed)"
  else
    echo "=========== Rouplex ============= Downloading java rpm $JDK8_RPM"
    wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/$JDK8_RPM -O $JDK8_RPM
    
    echo "=========== Rouplex ============= Installing java rpm $JDK8_NAME from $JDK8_RPM"
    sudo yum -y localinstall $JDK8_RPM

    if ! yum list installed $JDK8_NAME >/dev/null 2>&1; then
      echo "=========== Rouplex ============= Error installing rpm $JDK8_NAME Exiting"
      exit 1
    fi
  fi
}

download_tomcat() {
  echo "=========== Rouplex ============= Downloading tomcat ($TOMCAT8_GZ)"
  wget http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.12/bin/$TOMCAT8_GZ
}

install_tomcat() {
  echo "=========== Rouplex ============= Untaring tomcat ($TOMCAT8_GZ)"
  tar -xvf $TOMCAT8_GZ >/dev/null 2>&1
}

setup_tomcat() {
  install_tomcat
  if [ $? -ne 0 ]; then
    download_tomcat
    install_tomcat
  fi
}

download_combined() {
  echo "=========== Rouplex ============= Downloading $2 from ${S3_DEPLOY_FOLDER}/$2 to $3"
  $1 aws s3 cp ${S3_DEPLOY_FOLDER}/$2 $3 >/dev/null 2>&1

  if [ $? -ne 0 ]; then
    echo "=========== Rouplex ============= Downloading $2 failed. Trying from ${GITHUB_DEPLOY_FOLDER}/$2 to $3"
    $1 wget ${GITHUB_DEPLOY_FOLDER}/$2 -O $3 >/dev/null 2>&1
  fi

  if [ $? -ne 0 ]; then
    echo "=========== Rouplex ============= Downloading $2 failed. Trying from ${S3_DEFAULT_DEPLOY_FOLDER}/$2 to $3"
    $1 aws s3 cp ${S3_DEFAULT_DEPLOY_FOLDER}/$2 $3 >/dev/null 2>&1
  fi

  if [ $? -ne 0 ]; then
    echo "=========== Rouplex ============= Aborting setup. Failed locating $2"
    exit 1
  fi

  echo "=========== Rouplex ============= Downloaded $3"
}

setup_keystore() {
  download_combined "" ${SERVER_KEYSTORE} $TOMCAT8/conf/${SERVER_KEYSTORE}
  download_combined "" ${SERVER_KEYSTORE}.password $TOMCAT8/conf/${SERVER_KEYSTORE}.password
  SERVER_KEYSTORE_PASSWORD=`cat $TOMCAT8/conf/${SERVER_KEYSTORE}.password`

  echo "=========== Rouplex ============= Creating bin/setenv.sh"
  echo export JAVA_OPTS=\"-Djavax.net.ssl.keyStore=`pwd`/$TOMCAT8/conf/$SERVER_KEYSTORE -Djavax.net.ssl.keyStorePassword=$SERVER_KEYSTORE_PASSWORD -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=$HOST_NAME\" > $TOMCAT8/bin/setenv.sh
  chmod 700 $TOMCAT8/bin/setenv.sh

  echo "=========== Rouplex ============= Deleting $TOMCAT8/conf/${SERVER_KEYSTORE}.password"
  rm $TOMCAT8/conf/${SERVER_KEYSTORE}.password
}

setup_manager() {
  download_combined "" tomcat-users.xml $TOMCAT8/conf/tomcat-users.xml
  download_combined "" manager-context.xml $TOMCAT8/webapps/manager/META-INF/context.xml
}

setup_jmx() {
  echo "=========== Rouplex ============= Downloading tomcat extras catalina-jmx-remote.jar"
  wget http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.12/bin/extras/catalina-jmx-remote.jar -O $TOMCAT8/lib/catalina-jmx-remote.jar >/dev/null 2>&1

  download_combined "" server.xml $TOMCAT8/conf/server.xml
}

setup_initd() {
  download_combined "sudo" initd.tomcat /etc/init.d/tomcat
  sudo chmod 700 /etc/init.d/tomcat

  echo "=========== Rouplex ============= Making tomcat restartable on reboot"
  sudo chkconfig tomcat on
}

start_tomcat() {
  echo "=========== Rouplex ============= Starting tomcat"
  sudo chown -R ec2-user:ec2-user $TOMCAT8
  sudo service tomcat restart
}

init_setup $1 $2
setup_java
setup_tomcat
setup_keystore
setup_manager
setup_jmx
setup_initd
start_tomcat
