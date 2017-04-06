rouplex-benchmark-service
=======

# README #
This repo provides an J2EE web application built using rouplex-platform-jersey and for managing rouplex-platform
components (such as configuring/starting/stopping other services) as well as orchestration of complex scenarios for
testing and/or benchmarking.

## Description ##
This service will be used for testing and benchmarking various configurations of rouplex-platform services and
components. For now, it has support for rouplex-platform-tcp. The build artifact is a war that can be deployed in an
application container such as tomcat.

## Versioning ##
We use semantic versioning, in its representation x.y.z, x stands for API update, y for dependencies update, and z for
build number.

## Build ##

1. Java is required to build the project. Make sure the installation is successful by typing `java -version`; the
command output should show the version.

1. Maven is required to build the project. Please download and install it. Make sure the installation is successful by
typing `mvn -version` in a shell window; the command output should be showing the installation folder.

1. On a shell window, and from the folder containing this README.txt file, type `mvn clean install` and if
successful, you will have the built artifacts in appropriate 'target' folders located under the appropriate modules.
The same jars will be installed in your local maven repo.

## Test ##
`mvn test`

## Run ##
1. Tomcat or similar application container is required to run the project's webapp. Download and install it.
1. Copy the rouplex-benchmark-service/benchmark-service-provider/src/test/resources/server-keystore file on the host.
Let say you copied it on $TOMCAT_HOME/conf/server-keystore
1. Edit or create $TOMCAT_HOME/bin/setenv.sh and add this line to it:
export JAVA_OPTS="-Djavax.net.ssl.keyStore=$TOMCAT_HOME/conf/server-keystore -Djavax.net.ssl.keyStorePassword=kotplot"
1. Deploy the webapp by uploading it in tomcat (usually context path benchmark-service-provider-jersey-1.0-SNAPSHOT/)
1. Use the browser to get to url (for now: http://domain.com:8080/benchmark-service-provider-jersey-1.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=http://domain.com:8080/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/swagger.json)
1. The UI is not fancy but quite intuitive. Use something like this snippet to start a new echo server:

{
  "provider": "ROUPLEX_NIOSSL",
  "hostname": null,
  "port": 8888,
  "ssl": true,
  "useSharedBinder": false,
  "socketSendBufferSize": 0,
  "socketReceiveBufferSize": 0,
  "metricsAggregation": {
    "aggregateSslWithPlain": false,
    "aggregateServerAddresses": true,
    "aggregateServerPorts": true,
    "aggregateClientAddresses": true,
    "aggregateClientPorts": true
  },
  "backlog": 1000
}

The result will show the effective ip address (xx.xx.xx.xx), hostname, and port (pppp)

1. Use something like this snippet to start a 1000 clients sending requests and collecting responses from echo server:

{
  "provider": "ROUPLEX_NIOSSL",
  "hostname": "xx.xx.xx.xx",
  "port": pppp,
  "ssl": true,
  "useSharedBinder": false,
  "socketSendBufferSize": 0,
  "socketReceiveBufferSize": 0,
  "metricsAggregation": {
    "aggregateSslWithPlain": false,
    "aggregateServerAddresses": true,
    "aggregateServerPorts": true,
    "aggregateClientAddresses": true,
    "aggregateClientPorts": true
  },
  "clientCount": 1000,
  "minPayloadSize": 1000,
  "maxPayloadSize": 1001,
  "minDelayMillisBetweenSends": 100,
  "maxDelayMillisBetweenSends": 101,
  "minDelayMillisBeforeCreatingClient": 0,
  "maxDelayMillisBeforeCreatingClient": 10000,
  "minClientLifeMillis": 20000,
  "maxClientLifeMillis": 20001
}

If deciding to run locally (without deploying with a context path), point your browser at:
http://localhost:8080/webjars/swagger-ui/2.2.5/index.html?url=http://localhost:8080/rouplex/swagger.json

## Configure Host ##
If you will test with a high number of connections you must adjust the number of open file descriptors allowed for the
system and the user. A good link here: https://www.cyberciti.biz/faq/linux-increase-the-maximum-number-of-open-files/
but in short, you have to `sudo vi /etc/security/limits.conf` and add two entries at the end:

ec2-user         soft    nofile          888888
ec2-user         hard    nofile          999999

Log out and back in for settings to take effect.

## Configure Jmx ##
One of the flaws of JMX is that it will pick an available second port at random, and if your target machine is behind a
firewall, you're out of luck because you don't know which port to open up for jconsole to connect! Since tomcat 6, a
listener is available to force the creation in a predefined port (which you have already opened up). Per instructions
in this url: http://gabenell.blogspot.com/2010/04/connecting-to-jmx-on-tomcat-6-through.html, and tested with
tomcat 8.5.11:

1. Edit or create TOMCAT_HOME/bin/setenv.sh and add this line to it:
export JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=xx.xx.xx.xx"
1. Copy TOMCAT_HOME/bin/extras/catalina-jmx-remote.jar to TOMCAT_HOME/lib (or download it at http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.11/bin/extras/catalina-jmx-remote.jar if not available)
1. Edit TOMCAT_HOME/conf/server.xml and add listener:
<Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener" rmiRegistryPortPlatform="1706" rmiServerPortPlatform="1705"/>
1. Open ingress ports 1705 and 1706 in your network
1. Restart tomcat
1. Start jconsole and point it to
service:jmx:rmi://xx.xx.xx.xx:1705/jndi/rmi://xx.xx.xx.xx:1706/jmxrmi
1. Go to MBeans then metrics (make sure there is metrics first by starting a server and at least a client)

## Configure automatic start on system reboot on EC2 ##

1. As root user, copy the file at benchmark-service-provider-jersey/config/initd.tomcat.template to your host's
/etc/init.d/tomcat
2. Grant exec permission to /etc/init.d/tomcat
3. Exec shell command `sudo service tomcat restart` and the tomcat will be running with the new settings, now and on a
system reboot

The initd.tomcat.template is quite classic for starting tomcat servers, we are only adding a few CATALINA_OPS to set
appropriate values for the heap memory to be used, as well as provide a configuration value used by JMX listener.

# Use 80% of the free memory
free_mem_kb=`free -t | grep Mem | awk '{print $2}'`
use_mem_mb=$(( free_mem_kb * 4 / 5 / 1024 ))m

# Ec2 call to get the public ip address, which is needed to expose the jmx ip/port for jconsole to connect to
public_ipv4=`curl http://169.254.169.254/latest/meta-data/public-ipv4`

#CATALINA_OPS are the extra options for tomcat to get in
export CATALINA_OPTS="-Xmx$use_mem_mb -Djava.rmi.server.hostname=$public_ipv4"

## Contribution guidelines ##

* Writing tests
* Code review
* Other guidelines

## Who do I talk to? ##

* Repo owner or admin
andimullaraj@gmail.com