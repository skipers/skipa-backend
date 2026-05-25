package com.skipers.skipa.domain.auth.application;

import com.skipers.skipa.domain.auth.dto.request.LoginRequest;
import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("user_01")
                .name("User")
                .email("user@example.com")
                .password("encoded-password")
                .role(UserRole.BUSINESS)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void loginReturnsIssuedTokensAndAuthenticatedUserInformation() {
        when(userRepository.findByLoginId("user_01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtProvider.createAccessToken(1L, UserRole.BUSINESS)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(1L))
                .thenReturn(new JwtProvider.RefreshTokenResult("refresh-token", "refresh-jti"));

        LoginResponse response = authService.login(new LoginRequest("user_01", "password"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().loginId()).isEqualTo("user_01");
        assertThat(response.user().role()).isEqualTo("BUSINESS");
        assertThat(response.user().departmentId()).isNull();
    }

    @Test
    void loginRejectsUnknownUserWithoutCheckingPasswordOrIssuingTokens() {
        when(userRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertInvalidLoginRequest(() -> authService.login(new LoginRequest("missing", "password")));

        verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).createRefreshToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginRejectsIncorrectPasswordWithoutIssuingTokens() {
        when(userRepository.findByLoginId("user_01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertInvalidLoginRequest(() -> authService.login(new LoginRequest("user_01", "wrong-password")));

        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).createRefreshToken(org.mockito.ArgumentMatchers.any());
    }

    private void assertInvalidLoginRequest(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN_REQUEST));
    }
}
