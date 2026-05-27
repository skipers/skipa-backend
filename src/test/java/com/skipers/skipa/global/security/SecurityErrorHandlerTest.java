package com.skipers.skipa.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private ObjectMapper objectMapper;
    private CustomAuthenticationEntryPoint authenticationEntryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticationEntryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
    }

    @Test
    void authenticationEntryPointWritesUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("error").get("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void authenticationEntryPointWritesSpecificTokenError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(response, ErrorCode.EXPIRED_TOKEN);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("error").get("code").asText()).isEqualTo("EXPIRED_TOKEN");
    }

    @Test
    void accessDeniedHandlerWritesForbiddenResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("error").get("code").asText()).isEqualTo("FORBIDDEN");
    }
}
