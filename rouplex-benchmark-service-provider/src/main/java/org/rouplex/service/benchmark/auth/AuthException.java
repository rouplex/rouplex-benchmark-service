package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class AuthException extends Exception {
    public enum Reason {
        NotFound, BadCredentials, NotAuthorized
    }

    private final Reason reason;

    public AuthException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    Reason getReason() {
        return reason;
    }
}
