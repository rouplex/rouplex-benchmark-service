package org.rouplex.service;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.rouplex.platform.jersey.RouplexJerseyApplication;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.BenchmarkServiceResource;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import java.io.Closeable;
import java.io.IOException;

/**
 * This is the webapp, or the main jersey {@link javax.ws.rs.core.Application} which binds all the jersey resources.
 * The container searches for the {@link ApplicationPath} annotation and instantiates an instance of this class. It is
 * only in the constructor that we can bind (or add) resources to it, the jersey API does not allow for anything else.
 */
@ApplicationPath("/rouplex")
public class SecurityServiceApplication extends RouplexJerseyApplication implements ApplicationEventListener, Closeable {

    public SecurityServiceApplication(@Context ServletContext servletContext) {
        super(servletContext);

        bindResource(BenchmarkServiceResource.class, true);
    }

    @Override
    public void close() {
        for (RouplexTcpServer rouplexTcpServer : BenchmarkServiceProvider.rouplexTcpServers.values()) {
            try {
                rouplexTcpServer.close();
            } catch (IOException ioe) {
                // log it
            }
        }
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        switch (event.getType()) {
            case INITIALIZATION_FINISHED:
                System.out.println("Application "
                        + event.getResourceConfig().getApplicationName()
                        + " was initialized.");
                break;
            case DESTROY_FINISHED:
                close();
                break;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }
}
