package com.skipers.skipa.domain.auth.application;

import com.skipers.skipa.domain.auth.dto.request.LoginRequest;
import com.skipers.skipa.domain.auth.dto.request.RegisterRequest;
import com.skipers.skipa.domain.auth.dto.request.TokenRefreshRequest;
import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.auth.dto.response.MeResponse;
import com.skipers.skipa.domain.auth.dto.response.TokenRefreshResponse;
import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.global.exception.ErrorCode;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private RefreshTokenStore refreshTokenStore;

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
                .status(UserStatus.ACTIVE)
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
        verify(refreshTokenStore).save(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("refresh-token"),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void meReturnsCurrentUserInformation() {
        MeResponse response = authService.me(user);

        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().loginId()).isEqualTo("user_01");
        assertThat(response.user().role()).isEqualTo("BUSINESS");
        assertThat(response.user().departmentId()).isNull();
        assertThat(response.user().departmentName()).isNull();
    }

    @Test
    void refreshReturnsNewAccessTokenWhenStoredTokenMatches() {
        when(jwtProvider.getUserId("refresh-token")).thenReturn(1L);
        when(refreshTokenStore.matches(1L, "refresh-token")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(1L, UserRole.BUSINESS)).thenReturn("new-access-token");

        TokenRefreshResponse response = authService.refresh(new TokenRefreshRequest("refresh-token"));

        verify(jwtProvider).validateRefreshToken("refresh-token");
        assertThat(response.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    void refreshRejectsTokenMissingFromStore() {
        when(jwtProvider.getUserId("refresh-token")).thenReturn(1L);
        when(refreshTokenStore.matches(1L, "refresh-token")).thenReturn(false);

        assertErrorCode(
                () -> authService.refresh(new TokenRefreshRequest("refresh-token")),
                ErrorCode.INVALID_TOKEN
        );

        verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void logoutDeletesCurrentUsersRefreshToken() {
        authService.logout(user);

        verify(refreshTokenStore).delete(1L);
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

    @Test
    void loginRejectsPendingUserWithoutIssuingTokens() {
        User pendingUser = User.builder()
                .loginId("pending")
                .name("Pending User")
                .email("pending@example.com")
                .password("encoded-password")
                .role(UserRole.LEGAL)
                .build();
        when(userRepository.findByLoginId("pending")).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertErrorCode(
                () -> authService.login(new LoginRequest("pending", "password")),
                ErrorCode.PENDING_USER
        );

        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).createRefreshToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerEncodesPasswordAndCreatesPendingUser() {
        RegisterRequest request = registerRequest("LEGAL");
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getLoginId()).isEqualTo("new-user");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.LEGAL);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getDepartment()).isNull();
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void registerRejectsDuplicateLoginId() {
        when(userRepository.existsByLoginId("new-user")).thenReturn(true);

        assertErrorCode(() -> authService.register(registerRequest("LEGAL")), ErrorCode.DUPLICATE_LOGIN_ID);

        verify(userRepository, never()).existsByEmail(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("new-user@example.com")).thenReturn(true);

        assertErrorCode(() -> authService.register(registerRequest("LEGAL")), ErrorCode.DUPLICATE_EMAIL);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerRejectsAdminRole() {
        assertErrorCode(() -> authService.register(registerRequest("ADMIN")), ErrorCode.INVALID_ROLE);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerRejectsUnsupportedRole() {
        assertErrorCode(() -> authService.register(registerRequest("MANAGER")), ErrorCode.INVALID_ROLE);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void assertInvalidLoginRequest(Runnable invocation) {
        assertErrorCode(invocation, ErrorCode.INVALID_LOGIN_REQUEST);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private RegisterRequest registerRequest(String role) {
        return new RegisterRequest(
                "new-user",
                "password",
                "New User",
                "new-user@example.com",
                role
        );
    }
}
