package io.praharsh.payments.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final PaymentProperties properties;

    public ApiKeyFilter(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        if (supplied != null && MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                properties.getApiKey().getBytes(StandardCharsets.UTF_8)
        )) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"code":"unauthorized","message":"A valid X-API-Key header is required.","timestamp":"%s"}
                """.formatted(Instant.now()));
    }
}
