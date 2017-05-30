package org.rouplex.service;

import java.awt.*;
import java.net.URI;

/**
 * This is the helper class to be point to the base url for this resource.
 *
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class OpenInBrowser {
    public static void main(String[] args) throws Exception {
        Desktop.getDesktop().browse(URI.create("http://localhost:8080/benchmark-service-provider-jersey/webjars/swagger-ui/2.2.5/index.html?url=http://localhost:8080/benchmark-service-provider-jersey/rouplex/swagger.json"));
    }
}
