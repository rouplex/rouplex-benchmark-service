package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartTcpServerRequest extends StartTcpEndPointRequest {
    int backlog;
    String echoRatio;

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public String getEchoRatio() {
        return echoRatio;
    }

    public void setEchoRatio(String echoRatio) {
        this.echoRatio = echoRatio;
    }
}
