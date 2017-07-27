package org.rouplex.service;

import java.awt.*;
import java.net.URI;

/**
 * This is the helper class to point to the base url for this resource.
 *
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class OpenInBrowser {
    public static void main(String[] args) throws Exception {
        Desktop.getDesktop().browse(URI.create("http://localhost:8080/index.html"));
        Desktop.getDesktop().browse(URI.create("http://localhost:8080/webjars/swagger-ui/2.2.5/index.html?url=http://localhost:8080/rest/swagger.json"));
    }
}
