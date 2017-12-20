package org.rouplex;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Collections;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class JmxPoller {

    public static void main(String[] kot) throws Exception {
        new JmxPoller().pill();
    }

    void pill() throws Exception {
        RestClient restClient = RestClient.builder(
            new HttpHost("search-rouplex-demo-uflhstqfx4x2ifbelyqryzgyu4.us-west-2.es.amazonaws.com", 443, "https")
        ).build();

        Response response = restClient.performRequest("GET", "/", Collections.singletonMap("pretty", "true"));
        System.out.println(EntityUtils.toString(response.getEntity()));

//index a document
        HttpEntity entity = new NStringEntity(
            "{\n" +
                "    \"user\" : \"kimchy\",\n" +
                "    \"post_date\" : \"2009-11-15T14:12:12\",\n" +
                "    \"message\" : \"trying out Elasticsearch\"\n" +
                "}", ContentType.APPLICATION_JSON);

        Response indexResponse = restClient.performRequest(
            "PUT",
            "/twitter/tweet/1",
            Collections.<String, String>emptyMap(),
            entity);


        // create jmx connection with mules jmx agent
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url);
        //jmxc.connect();
        MBeanServerConnection connection = jmxc.getMBeanServerConnection();

        for (ObjectInstance metric : connection.queryMBeans(new ObjectName("rouplex-benchmark:*"), null)) {
            for (MBeanAttributeInfo mbAttributeInfo : connection.getMBeanInfo(metric.getObjectName()).getAttributes()) {
                System.out.println(metric.getObjectName() + " " + mbAttributeInfo.toString());
            }
        }

        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < 100000; j *= 10) {
                String objName = String.format("rouplex-benchmark:name=\"CLASSIC_NIO.A.EchoRequester.A:A::A:A.connectionTime.millisBucket.%s\"", j);
                Object count = connection.getAttribute(new ObjectName(objName), "Count");
                System.out.println(count);
            }

            Thread.sleep(1000);
        }
//create object instances that will be used to get memory and operating system Mbean objects exposed by JMX; create variables for cpu time and system time before
        Object memoryMbean = null;
        Object osMbean = null;
        long cpuBefore = 0;
        long tempMemory = 0;
        CompositeData cd = null;

// call the garbage collector before the test using the Memory Mbean
        jmxc.getMBeanServerConnection().invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);

        int samplesCount = 3;
//create a loop to get values every second (optional)
        for (int i = 0; i < samplesCount; i++) {

//get an instance of the HeapMemoryUsage Mbean
            memoryMbean = connection.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
            cd = (CompositeData) memoryMbean;
//get an instance of the OperatingSystem Mbean
            osMbean = connection.getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "ProcessCpuTime");
            System.out.println("Used memory: " + " " + cd.get("used") + " Used cpu: " + osMbean); //print memory usage
            tempMemory = tempMemory + Long.parseLong(cd.get("used").toString());
            Thread.sleep(1000); //delay for one second
        }

//get system time and cpu time from last poll
        long cpuAfter = Long.parseLong(osMbean.toString());

        long cpuDiff = cpuAfter - cpuBefore; //find cpu time between our first and last jmx poll
        System.out.println("Cpu diff in milli seconds: " + cpuDiff / 1000000); //print cpu time in miliseconds
        System.out.println("average memory usage is: " + tempMemory / samplesCount);//print average memory usage
    }

    void poll() throws Exception {
        // create jmx connection with mules jmx agent
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        jmxc.connect();

//create object instances that will be used to get memory and operating system Mbean objects exposed by JMX; create variables for cpu time and system time before
        Object memoryMbean = null;
        Object osMbean = null;
        long cpuBefore = 0;
        long tempMemory = 0;
        CompositeData cd = null;

// call the garbage collector before the test using the Memory Mbean
        jmxc.getMBeanServerConnection().invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);

        int samplesCount = 10;
//create a loop to get values every second (optional)
        for (int i = 0; i < samplesCount; i++) {

//get an instance of the HeapMemoryUsage Mbean
            memoryMbean = jmxc.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
            cd = (CompositeData) memoryMbean;
//get an instance of the OperatingSystem Mbean
            osMbean = jmxc.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "ProcessCpuTime");
            System.out.println("Used memory: " + " " + cd.get("used") + " Used cpu: " + osMbean); //print memory usage
            tempMemory = tempMemory + Long.parseLong(cd.get("used").toString());
            Thread.sleep(1000); //delay for one second
        }

//get system time and cpu time from last poll
        long cpuAfter = Long.parseLong(osMbean.toString());

        long cpuDiff = cpuAfter - cpuBefore; //find cpu time between our first and last jmx poll
        System.out.println("Cpu diff in milli seconds: " + cpuDiff / 1000000); //print cpu time in miliseconds
        System.out.println("average memory usage is: " + tempMemory / samplesCount);//print average memory usage
    }
}
