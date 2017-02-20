package org.rouplex.service.benchmarkservice;

import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.platform.rr.SyncReplyService;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerResponse;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class BenchmarkServiceProvider implements BenchmarkService {
    public static Map<Integer, RouplexTcpServer> rouplexTcpServers = new HashMap<>();

    HttpServletRequest httpServletRequest;
    RouplexSecurityContext rouplexSecurityContext;
    HttpServletResponse httpServletResponse;

    public BenchmarkServiceProvider(HttpServletRequest httpServletRequest,
            RouplexSecurityContext rouplexSecurityContext, HttpServletResponse httpServletResponse) {
        this.httpServletRequest = httpServletRequest;
        this.rouplexSecurityContext = rouplexSecurityContext;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public PingResponse ping() {
        return buildHttpRequestResponse(null);
    }

    @Override
    public PingResponse ping(PingRequest pingRequest) {
        return buildHttpRequestResponse(pingRequest.getUser());
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        if (rouplexTcpServers.get(request.getPort()) != null) {
            throw new IOException("Another RouplexTcpServer is already serving using port " + request.getPort());
        }

        RouplexTcpServer rouplexTcpServer = new RouplexTcpServer()
                .withLocalAddress(request.getHostname(), request.getPort())
                .withServiceProvider(new SyncReplyService<byte[], ByteBuffer>() {
                    @Override
                    public ByteBuffer serviceRequest(byte[] request) {
                        return ByteBuffer.wrap(request); // echo
                    }
                })
                .start();

        InetSocketAddress inetSocketAddress = (InetSocketAddress) rouplexTcpServer.getLocalAddress();
        rouplexTcpServers.put(inetSocketAddress.getPort(), rouplexTcpServer);

        StartTcpServerResponse response = new StartTcpServerResponse();
        response.setHostname(inetSocketAddress.getHostName());
        response.setPort(inetSocketAddress.getPort());
        return response;
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        RouplexTcpServer rouplexTcpServer = rouplexTcpServers.remove(request.getPort());
        if (rouplexTcpServer == null) {
            throw new IOException("There is no RouplexTcpServer listening at port " + request.getPort());
        }

        rouplexTcpServer.close();

        return new StopTcpServerResponse();
    }

    private PingResponse buildHttpRequestResponse(String payload) {
        PingResponse pingResponse = new PingResponse();
        pingResponse.setHttpRequest(buildHttpRequest(payload));
        pingResponse.setSecurityContext(buildSecurityContext());
        pingResponse.setHttpResponse(buildHttpResponse());

        return pingResponse;
    }

    private HttpRequest buildHttpRequest(String payload) {
        HttpRequest result = new HttpRequest();

        result.setAuthType(httpServletRequest.getAuthType());

        SortedMap<String, List<String>> headers = new TreeMap<>();
        for (Enumeration<String> headerNames = httpServletRequest.getHeaderNames(); headerNames.hasMoreElements();) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, Collections.list(httpServletRequest.getHeaders(headerName)));
        }

        result.setHeaders(headers);
        result.setMethod(httpServletRequest.getMethod());
        result.setPathInfo(httpServletRequest.getPathInfo());
        result.setPathTranslated(httpServletRequest.getPathTranslated());
        result.setContextPath(httpServletRequest.getContextPath());
        result.setQueryString(httpServletRequest.getQueryString());
        result.setRemoteUser(httpServletRequest.getRemoteUser());
        result.setUserPrincipal(buildPrincipal(httpServletRequest.getUserPrincipal()));
        result.setRequestedSessionId(httpServletRequest.getRequestedSessionId());
        result.setRequestURI(httpServletRequest.getRequestURI());
        result.setRequestURL(httpServletRequest.getRequestURL().toString());
        result.setServletPath(httpServletRequest.getServletPath());
        result.setRequestedSessionIdValid(httpServletRequest.isRequestedSessionIdValid());
        result.setRequestedSessionIdFromCookie(httpServletRequest.isRequestedSessionIdFromCookie());
        result.setRequestedSessionIdFromURL(httpServletRequest.isRequestedSessionIdFromURL());

        result.setPayload(payload);

        return result;
    }

    private HttpResponse buildHttpResponse() {
        HttpResponse result = new HttpResponse();

        result.setCharacterEncoding(httpServletResponse.getCharacterEncoding());
        result.setContentType(httpServletResponse.getContentType());
        result.setLocale(httpServletResponse.getLocale().toString());

        return result;
    }

    private SecurityContext buildSecurityContext() {
        SecurityContext result = new SecurityContext();

        result.setAuthenticated(rouplexSecurityContext.isAuthenticated());
        result.setAuthenticationScheme(rouplexSecurityContext.getAuthenticationScheme());
        result.setSecure(rouplexSecurityContext.isSecure());
        result.setUserPrincipal(buildPrincipal(rouplexSecurityContext.getUserPrincipal()));
        result.setUserX509Certificate(rouplexSecurityContext.getUserX509Certificate());

        return result;
    }

    private Principal buildPrincipal(java.security.Principal principal) {
        if (principal == null) {
            return null;
        }

        Principal result = new Principal();
        result.setName(principal.getName());
        return result;
    }
}