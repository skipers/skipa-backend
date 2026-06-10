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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthException(ErrorCode.PENDING_USER);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        JwtProvider.RefreshTokenResult refreshTokenResult = jwtProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(
                user.getId(),
                refreshTokenResult.token(),
                Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis())
        );

        return LoginResponse.of(
                accessToken,
                refreshTokenResult.token(),
                user
        );
    }

    public MeResponse me(User user) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(ErrorCode.PENDING_USER);
        }

        return MeResponse.from(currentUser);
    }

    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();
        jwtProvider.validateRefreshToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        if (!refreshTokenStore.matches(userId, refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(ErrorCode.PENDING_USER);
        }

        return new TokenRefreshResponse(jwtProvider.createAccessToken(user.getId(), user.getRole()));
    }

    public void logout(User user) {
        refreshTokenStore.delete(user.getId());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new AuthException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException(ErrorCode.DUPLICATE_EMAIL);
        }

        UserRole role;
        try {
            role = UserRole.from(request.role());
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.INVALID_ROLE);
        }

        if (role == UserRole.ADMIN) {
            throw new AuthException(ErrorCode.INVALID_ROLE);
        }

        User user = User.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .role(role)
                .build();

        userRepository.save(user);

        return UserResponse.from(user);
    }
}
