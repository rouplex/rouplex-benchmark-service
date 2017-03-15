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
1. Tomcat or similar application container is required to run the project's webapp. Please download and install it.
1. Deploy the webapp by uploading it in tomcat
1. Use the browser to get to url (for now: http://domain.com:8080/benchmark-service-provider-jersey-0.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=http://domain.com:8080/benchmark-service-provider-jersey-0.0-SNAPSHOT/rouplex/swagger.json)
1. The UI is not fancy but quite intuitive.
Use something like this snippet to start a new echo server:

{
  "useNiossl": true,
  "hostname": null,
  "port": 8888,
  "ssl": true,
  "useSharedBinder": true,
  "mergeClientMetrics": true
}

Use something like this snippet to start a 100 clients sending requests and collecting responses from echo server:

{
  "useNiossl": true,
  "hostname": **server_name_as_returned_by_start_server_command**,
  "port": 8888,
  "ssl": true,
  "useSharedBinder": true,
  "mergeClientMetrics": true,
  "clientCount": 100,
  "minPayloadSize": 1,
  "maxPayloadSize": 10000,
  "minDelayMillisBetweenSends": 10,
  "maxDelayMillisBetweenSends": 100,
  "minDelayMillisBeforeCreatingClient": 10,
  "maxDelayMillisBeforeCreatingClient": 1000,
  "minClientLifeMillis": 10000,
  "maxClientLifeMillis": 60000
}

## Configure Jmx ##
One of the flaws of JMX is that it will pick an available second port at random, and if your target machine is behind a
firewall, you're out of luck because you don't know which port to open up (for jconsole to connect)! Since tomcat 6, a
listener is available to force the creation in a predefined port (which you have already opened up). Per instructions
in this url: http://gabenell.blogspot.com/2010/04/connecting-to-jmx-on-tomcat-6-through.html, and tested with
tomcat 8.5.11:

1. Edit or create TOMCAT_HOME/bin/setenv.sh and add this line to it:
export JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=xx.xx.xx.xx"
1. Copy TOMCAT_HOME/bin/extras/catalina-jmx-remote.jar to TOMCAT_HOME/lib (or download it at http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.11/bin/extras/catalina-jmx-remote.jar if not available)
1. Edit TOMCAT_HOME/conf/server.xml and add listener:
<Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener" rmiServerPortPlatform="10001" rmiRegistryPortPlatform="10002"/>
1. Open ports 10001 and 10002 in your network
1. Restart tomcat
1. Start jconsole and point it to
service:jmx:rmi://xx.xx.xx.xx:10001/jndi/rmi://xx.xx.xx.xx:10002/jmxrmi
1. Go to MBeans then metrics (make sure there is metrics first by starting a server and at least a client)

## Contribution guidelines ##

* Writing tests
* Code review
* Other guidelines

## Who do I talk to? ##

* Repo owner or admin
andimullaraj@gmail.com