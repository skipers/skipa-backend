package com.skipers.skipa.domain.auth.application;

import com.skipers.skipa.domain.auth.dto.request.LoginRequest;
import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import com.skipers.skipa.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findById(request.id())
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_LOGIN_REQUEST));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        JwtProvider.RefreshTokenResult refreshTokenResult = jwtProvider.createRefreshToken(user.getId());

        return LoginResponse.of(
                accessToken,
                refreshTokenResult.token(),
                user
        );
    }
}
