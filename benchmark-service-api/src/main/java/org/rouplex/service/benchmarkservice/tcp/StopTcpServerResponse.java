package org.rouplex.service.benchmarkservice.tcp;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StopTcpServerResponse {
    int responseCode;
    String responseMessage;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
