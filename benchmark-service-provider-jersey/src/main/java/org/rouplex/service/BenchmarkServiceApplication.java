package org.rouplex.service;

import org.rouplex.nio.channels.spi.SSLSelectorProvider;
import org.rouplex.platform.jersey.RouplexJerseyApplication;
import org.rouplex.service.benchmarkservice.BenchmarkServiceResource;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import java.io.Closeable;

/**
 * This is the webapp, or the main jersey {@link javax.ws.rs.core.Application} which binds all the jersey resources.
 * The container searches for the {@link ApplicationPath} annotation and instantiates an instance of this class. It is
 * only in the constructor that we can bind (or add) resources to it, the jersey API does not allow for anything else.
 */
@ApplicationPath("/rouplex")
public class BenchmarkServiceApplication extends RouplexJerseyApplication implements Closeable { // todo ApplicationEventListener

    public BenchmarkServiceApplication(@Context ServletContext servletContext) {
        super(servletContext);
//        SSLSelectorProvider.provider();
//
//        try {
//            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("org.rouplex.nio.channels.spi.SSLSelectorProvider");
//            SSLSelectorProvider sslSelectorProvider = (SSLSelectorProvider) clazz.newInstance();
//            SSLSelectorProvider sslSelectorProvider1 = sslSelectorProvider;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        bindRouplexResource(BenchmarkServiceResource.class, true);
//        getSwaggerBeanConfig().setPrettyPrint(true);

        getSwaggerBeanConfig().setDescription(
                "jconsole service:jmx:rmi://www.rouplex-demo.com:1705/jndi/rmi://www.rouplex-demo.com:1706/jmxrmi");
    }

    @Override
    public void close() {
//        for (RouplexTcpServer rouplexTcpServer : BenchmarkServiceProvider.benchmarkTcpServers.values()) {
//            try {
//                rouplexTcpServer.close();
//            } catch (IOException ioe) {
//                // log it
//            }
//        }
    }

// todo check app lifecycle
//    @Override
//    public void onEvent(ApplicationEvent event) {
//        switch (event.getType()) {
//            case INITIALIZATION_FINISHED:
//                System.out.println("Application "
//                        + event.getResourceConfig().getApplicationName()
//                        + " was initialized.");
//                break;
//            case DESTROY_FINISHED:
//                close();
//                break;
//        }
//    }
//
//    @Override
//    public RequestEventListener onRequest(RequestEvent requestEvent) {
//        return null;
//    }
}
