package org.conrad.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Adds {@code X-Served-By: <service>@<host>:<port>} to every response, so it is visible which
 * instance answered. That is what makes load balancing easy to demonstrate: call the same endpoint
 * many times and count the values. In Docker the host is the container id. Through the gateway a
 * request shows two values, the gateway's and the service's. Switch off with
 * {@code app.served-by-header=false}.
 */
@Component
@ConditionalOnProperty(name = "app.served-by-header", havingValue = "true", matchIfMissing = true)
public class ServedByFilter extends OncePerRequestFilter {

    private final String instance;

    public ServedByFilter(@Value("${spring.application.name:service}") String application,
                          @Value("${server.port:8080}") String port) {
        this.instance = application + "@" + hostName() + ":" + port;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.addHeader("X-Served-By", instance);
        chain.doFilter(request, response);
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
