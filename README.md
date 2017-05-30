rouplex-benchmark-service
=======

This repo provides a web application built using rouplex-platform-jersey and for managing rouplex-platform components
(for configuring/starting/stopping service providers and consumers) as well as orchestration of complex scenarios for
testing and/or benchmarking.

# Description #
This service will be used for testing and benchmarking various configurations of rouplex-platform services and
components. For now, it has support for rouplex-platform-tcp. The build artifact is a war that can be deployed in an
application container such as tomcat.

# Versioning #
We use semantic versioning, in its representation x.y.z, x stands for API update, y for dependencies update, and z for
build number.

# Build #
1. Java is required to build the project. Make sure the installation is successful by typing `java -version` on a shell
window; the command output should show the version.

1. Maven is required to build the project. Make sure the installation is successful by typing `mvn -version` on a shell
window; the command output should be showing the installation folder.

1. On a shell window, and from the folder containing this README file, type `mvn clean install` and if successful, you
will have the built artifacts in appropriate 'target' folders located under the appropriate modules. The same jars will
be installed in your local maven repo.

# Test #
`mvn test` will execute all the tests and the console output should show success upon finishing.

# Run #
To run locally and mostly for debugging purposes, type `cd benchmark-service-provider-jersey; mvn tomcat7:run` then
point your browser at http://localhost:8080/benchmark-service-provider-jersey/webjars/swagger-ui/2.2.5/index.html?url=http://localhost:8080/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/swagger.json
Refer to the API section for details on requests and related responses.

# Deploy #
To deploy and run remotely on an App server you must make sure you follow these steps:

1. Java8 will be needed to run the benchmark service on your host(s). You can get it via `wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u102-b14/jdk-8u102-linux-x64.rpm; sudo yum localinstall jdk-8u102-linux-x64.rpm`

1. An application container is required to run the service. You can download tomcat if none is available on your host.
`wget http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.12/bin/apache-tomcat-8.5.12.tar.gz; tar -xvf apache-tomcat-8.5.12.tar.gz`

1. A server key and certificate is required to run the test servers. You can create your own or you can copy the
keystore at rouplex-benchmark-service/benchmark-service-provider/src/test/resources/server-keystore somewhere on your
host. Let say you copied it on $TOMCAT_HOME/conf/server-keystore. The keystore password is "kotplot" without the quotes.

1. The test servers must be configured to find the geoLocation of the keystore. That can be done by editing
(or creating) $TOMCAT_HOME/bin/setenv.sh file to add the line containing the system properties used by JVM for this purpose `export JAVA_OPTS="-Djavax.net.ssl.keyStore=$TOMCAT_HOME/conf/server-keystore -Djavax.net.ssl.keyStorePassword=kotplot"`

1. The application container must be started ($TOMCAT_HOME/catalina.sh start is one way of doing it) for a dynamic
deployment (or one can opt for a static deployment, equivalent, but out of the scope of this guide)

1. You must now deploy the benchmark service to the application container. Point your browser at
`http://domain.com:8080/manager/html` and you should see the tomcat manager page.
  * If you get permission denied, it is because your manager by default is configured to allow only local connections.
  You can override that behaviour by editting manager's config `vi $TOMCAT_HOME/webapps/manager/META-INF/context.xml`
  and lifting the restriction by commenting out the valve, or restrict to your public ip address (not shown).
```xml
<Context antiResourceLocking="false" privileged="true" >
    <!-- <Valve className="org.apache.catalina.valves.RemoteAddrValve" allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" /> -->
</Context>
```
  * The role, username and password for the admin are set via `vi $TOMCAT_HOME/conf/tomcat-users.xml`. Make sure you have something like
```xml
<tomcat-users>
<role rolename="manager-gui"/>
<user username="tomcat" password="<password>" roles="manager-gui"/>
</tomcat-users>
```
1. Deploy the benchmark service by uploading it in tomcat via deploy button (context path will be: benchmark-service-provider-jersey-1.0.0-SNAPSHOT/).

1. Use the browser to get to url (http://domain.com:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=http://domain.com:8080/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/swagger.json)

1. The UI is not fancy but quite intuitive. You can click the yellowish area on the right side to have a template copied
on the left which you can then modify prior to trying it via the Try button. Use something like this snippet to start a
new echo server:

```json
{
  "provider": "ROUPLEX_NIOSSL",
  "hostname": null,
  "port": 8888,
  "ssl": true,
  "useSharedBinder": false,
  "optionalSocketSendBufferSize": 0,
  "optionalSocketReceiveBufferSize": 0,
  "metricsAggregation": {
    "aggregateSslWithPlain": false,
    "aggregateServerAddresses": true,
    "aggregateServerPorts": true,
    "aggregateClientAddresses": true,
    "aggregateClientPorts": true
  },
  "optionalBacklog": 1000
}
```
The result will show the effective ip address (xx.xx.xx.xx), hostname, and port (pppp)

1. Use something like this snippet to start a 1000 clients sending requests and collecting responses from echo server:
```json
{
  "provider": "ROUPLEX_NIOSSL",
  "hostname": "xx.xx.xx.xx",
  "port": pppp,
  "ssl": true,
  "useSharedBinder": false,
  "optionalSocketSendBufferSize": 0,
  "optionalSocketReceiveBufferSize": 0,
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
```

# Configure Host (Optional) #

## File Descriptors ##
If you will test with a high number of connections you must adjust the number of open file descriptors allowed for the
system and the user. A good link here: https://www.cyberciti.biz/faq/linux-increase-the-maximum-number-of-open-files/
but in short, you have to `sudo vi /etc/security/limits.conf` and add two entries at the end:

    ec2-user         soft    nofile          888888
    ec2-user         hard    nofile          999999

Log out and back in for settings to take effect.

## Jmx ##
Jmx is a great way to collect app metrics and somewhat visualize related graphs. The benchmark service already injects metrics
prefixed by "metrics" word which can be collected via jconsole or other jmx clients.

One of the flaws of JMX though, is that it will pick an available second port at random, and if your target machine is behind a firewall, you're out of luck because you don't know which port to open up for jconsole to connect! Since tomcat 6, a
listener is available to force the creation in a predefined port (which you have already opened up). Per instructions
in this url: http://gabenell.blogspot.com/2010/04/connecting-to-jmx-on-tomcat-6-through.html, and tested with
tomcat 8.5.11:

1. Edit or create TOMCAT_HOME/bin/setenv.sh and add this line to it (xx.xx.xx.xx is the public ip or host name)
```
export JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=xx.xx.xx.xx"
```
1. Copy TOMCAT_HOME/bin/extras/catalina-jmx-remote.jar to TOMCAT_HOME/lib (or download it at http://archive.apache.org/dist/tomcat/tomcat-8/v8.5.11/bin/extras/catalina-jmx-remote.jar if not available)
1. Edit TOMCAT_HOME/conf/server.xml and add listener
```xml
<Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener" rmiRegistryPortPlatform="1706" rmiServerPortPlatform="1705"/>
```
1. Open ingress ports 1705 and 1706 in your network (security groups in EC2)
1. Restart tomcat
1. Start jconsole and point it to
service:jmx:rmi://xx.xx.xx.xx:1705/jndi/rmi://xx.xx.xx.xx:1706/jmxrmi
1. Go to MBeans tab, then to metrics node in the tree (make sure there are metrics available first by starting a server and at least a client)

## Tomcat as an init.d service ##
1. As root user, copy the file at benchmark-service-provider-jersey/config/initd.tomcat.template to your host's /etc/init.d/tomcat
1. Grant exec permission to /etc/init.d/tomcat
1. Exec shell command `sudo service tomcat restart` and the tomcat will be running with the new settings, now and on a
system reboot

The initd.tomcat.template is quite classic for starting tomcat servers, we are only adding a few CATALINA_OPS to set
appropriate values for the heap memory to be used, as well as provide a configuration value used by JMX listener.
```
# Use 80% of the free memory
free_mem_kb=`free -t | grep Mem | awk '{print $2}'`
use_mem_mb=$(( free_mem_kb * 4 / 5 / 1024 ))m

# Ec2 call to get the public ip address, which is needed to expose the jmx ip/port for jconsole to connect to
public_ipv4=`curl http://169.254.169.254/latest/meta-data/public-ipv4`

#CATALINA_OPS are the extra options for tomcat to get in
export CATALINA_OPTS="-Xmx$use_mem_mb -Djava.rmi.server.hostname=$public_ipv4"
```

# Contribution guidelines #

* Writing tests
* Code review
* Other guidelines

# Who do I talk to? #

* Repo owner or admin
andimullaraj@gmail.com
