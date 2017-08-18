package org.rouplex.service.benchmark.orchestrator;

import java.util.regex.Pattern;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class ValidationUtilsExtension {
    private static Pattern IP_ADDRESS_PATTERN = Pattern.compile(
        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static void checkIpAddress(String ipAddress, String fieldName) {
        if (ipAddress == null) {
            throw new IllegalArgumentException(String.format("Argument %s cannot be null", fieldName));
        }

        if (!IP_ADDRESS_PATTERN.matcher(ipAddress).matches()) {
            throw new IllegalArgumentException(String.format("Argument %s is not an ip address", fieldName));
        }
    }
}
