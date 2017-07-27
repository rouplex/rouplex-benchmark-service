package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class AuthException extends Exception {
    enum Reason {
        NotFound, BadCredentials
    }

    private final Reason reason;

    AuthException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    Reason getReason() {
        return reason;
    }
}
