package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class NotAuthorizedException extends Exception {
    public final String url;

    public NotAuthorizedException(String url) {
        this.url = url;
    }
}
