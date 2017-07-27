package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class UserAtGoogle extends UserAtProvider {
    String givenName;
    String familyName;

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
}
