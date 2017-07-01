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
        Desktop.getDesktop().browse(URI.create(
            "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/index.html"));
    }
}
