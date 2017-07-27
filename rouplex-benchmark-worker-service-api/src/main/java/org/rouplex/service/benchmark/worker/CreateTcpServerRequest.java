package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class CreateTcpServerRequest extends CreateTcpEndPointRequest {
    private int backlog;
    private String echoRatio;

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
