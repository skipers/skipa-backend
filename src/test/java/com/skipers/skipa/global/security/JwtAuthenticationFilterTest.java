package com.skipers.skipa.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"INVALID_TOKEN", "EXPIRED_TOKEN"})
    void preservesTokenValidationErrorCodeInUnauthorizedResponse(ErrorCode errorCode) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JwtAuthenticationFilter filter = createFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new AuthException(errorCode)).when(jwtProvider).validateAccessToken("token");

        filter.doFilter(request, response, filterChain);

        JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.path("error").path("code").asText()).isEqualTo(errorCode.getCode());
        verifyNoInteractions(customUserDetailsService, filterChain);
    }

    @Test
    void requestWithoutBearerTokenContinuesWithoutAuthenticationAttempt() throws Exception {
        JwtAuthenticationFilter filter = createFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, customUserDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void loginRequestSkipsAuthenticationEvenWhenExpiredBearerTokenIsPresent() throws Exception {
        JwtAuthenticationFilter filter = createFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, customUserDetailsService);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void registerRequestSkipsAuthenticationEvenWhenExpiredBearerTokenIsPresent() throws Exception {
        JwtAuthenticationFilter filter = createFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        request.setServletPath("/auth/register");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, customUserDetailsService);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void validAccessTokenAuthenticatesUserAndContinuesFilterChain() throws Exception {
        JwtAuthenticationFilter filter = createFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails userDetails = new User(
                "user_01",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_BUSINESS"))
        );
        when(jwtProvider.getUserId("access-token")).thenReturn(1L);
        when(customUserDetailsService.loadUserByUsername("1")).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        verify(jwtProvider).validateAccessToken("access-token");
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(userDetails);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUSINESS");
    }

    @Test
    void existingAuthenticationContinuesWithoutAuthenticatingBearerTokenAgain() throws Exception {
        JwtAuthenticationFilter filter = createFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, customUserDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    private JwtAuthenticationFilter createFilter(ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(
                jwtProvider,
                customUserDetailsService,
                new CustomAuthenticationEntryPoint(objectMapper)
        );
    }
}
