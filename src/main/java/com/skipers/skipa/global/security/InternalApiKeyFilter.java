package com.skipers.skipa.global.security;

import com.skipers.skipa.global.config.WebConfig;
import com.skipers.skipa.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = WebConfig.API_PREFIX + "/internal/";

    @Value("${app.internal.api-key:}")
    private String internalApiKey;

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !resolvePath(request).startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestApiKey = request.getHeader(INTERNAL_API_KEY_HEADER);

        if (!isValidApiKey(requestApiKey)) {
            authenticationEntryPoint.commence(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidApiKey(String requestApiKey) {
        if (!StringUtils.hasText(internalApiKey) || !StringUtils.hasText(requestApiKey)) {
            return false;
        }

        byte[] expected = internalApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = requestApiKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private String resolvePath(HttpServletRequest request) {
        if (StringUtils.hasText(request.getServletPath())) {
            return request.getServletPath();
        }

        return request.getRequestURI();
    }
}
