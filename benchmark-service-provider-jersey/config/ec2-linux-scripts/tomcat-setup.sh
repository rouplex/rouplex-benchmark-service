init_setup() {
  if `uname` -neq "Linux"; then
    echo "=========== Rouplex ============= Aborting setup. Cause: Only Linux is supported by this setup"
    exit 1
  fi

  if `uname -m` -neq "x86_64"; then
    echo "=========== Rouplex ============= Aborting setup. Cause: Only 64 bit architectures supported by this setup"
    exit 1
  fi

  JDK8_NAME=jdk1.8.0_121.x86_64
  JDK8_RPM="jdk-8u121-linux-x64.rpm"
  TOMCAT8="apache-tomcat-8.5.12"
  TOMCAT8_GZ=$TOMCAT8.tar.gz
  HOST_NAME="www.rouplex-demo.com"
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

setup_keystore() {
  echo "=========== Rouplex ============= Downloading conf/server-keystore"
  aws s3 cp s3://rouplex/deploys/server-keystore $TOMCAT8/conf/server-keystore

  echo "=========== Rouplex ============= Creating bin/setenv.sh"
  echo export JAVA_OPTS=\"-Djavax.net.ssl.keyStore=`pwd`/$TOMCAT8/conf/server-keystore -Djavax.net.ssl.keyStorePassword=kotplot -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=$HOST_NAME\" > $TOMCAT8/bin/setenv.sh

  echo "=========== Rouplex ============= Granting exec permission to bin/setenv.sh"
  chmod 700 $TOMCAT8/bin/setenv.sh
}

setup_manager() {
  echo "=========== Rouplex ============= Downloading conf/tomcat-users.xml"
  aws s3 cp s3://rouplex/deploys/tomcat-users.xml $TOMCAT8/conf

  echo "=========== Rouplex ============= Downloading webapps/manager/META-INF/context.xml"
  aws s3 cp s3://rouplex/deploys/manager-context.xml $TOMCAT8/webapps/manager/META-INF/context.xml
}

setup_jmx() {
  echo "=========== Rouplex ============= Downloading tomcat extras catalina-jmx-remote.jar"
  wget http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.12/bin/extras/catalina-jmx-remote.jar -O $TOMCAT8/lib/catalina-jmx-remote.jar

  echo "=========== Rouplex ============= Downloading conf/server.xml"
  aws s3 cp s3://rouplex/deploys/server.xml $TOMCAT8/conf
}

setup_initd() {
  echo "=========== Rouplex ============= Downloading /etc/init.d/tomcat"
  sudo aws s3 cp s3://rouplex/deploys/initd.tomcat.template /etc/init.d/tomcat

  echo "=========== Rouplex ============= Granting exec permission to /etc/init.d/tomcat"
  sudo chmod 700 /etc/init.d/tomcat
}

start_tomcat() {
  echo "=========== Rouplex ============= Starting tomcat"
  sudo service tomcat restart
}

init_setup
setup_java
setup_tomcat
setup_keystore
setup_manager
setup_jmx
setup_initd
start_tomcat
