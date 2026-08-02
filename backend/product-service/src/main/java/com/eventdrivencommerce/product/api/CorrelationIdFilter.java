package com.eventdrivencommerce.product.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";
    public static final String CAUSATION_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".causationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID correlationId = parseOrCreate(request.getHeader(CORRELATION_HEADER));
        UUID causationId = UUID.randomUUID();
        request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
        request.setAttribute(CAUSATION_ATTRIBUTE, causationId);
        response.setHeader(CORRELATION_HEADER, correlationId.toString());
        MDC.put("correlationId", correlationId.toString());
        MDC.put("requestId", causationId.toString());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("requestId");
        }
    }

    private static UUID parseOrCreate(String value) {
        if (value != null && value.length() <= 36) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                // Invalid external correlation values are replaced rather than propagated.
            }
        }
        return UUID.randomUUID();
    }
}
