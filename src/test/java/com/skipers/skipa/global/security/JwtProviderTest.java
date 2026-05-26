package com.skipers.skipa.global.security;

import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "c2tpcGEtdGVzdC1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcy0xMjM0NTY=";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiration", 600000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiration", 604800000L);
        jwtProvider.init();
    }

    @Test
    void accessTokenContainsUserAndRoleAndPassesAccessValidation() {
        String token = jwtProvider.createAccessToken(1L, UserRole.LEGAL);

        jwtProvider.validateAccessToken(token);

        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.LEGAL);
    }

    @Test
    void refreshTokenContainsIdentifierAndCannotBeUsedAsAccessToken() {
        JwtProvider.RefreshTokenResult result = jwtProvider.createRefreshToken(1L);

        jwtProvider.validateRefreshToken(result.token());

        assertThat(jwtProvider.getUserId(result.token())).isEqualTo(1L);
        assertThat(jwtProvider.getJti(result.token())).isEqualTo(result.jti());
        assertErrorCode(() -> jwtProvider.validateAccessToken(result.token()), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String token = jwtProvider.createAccessToken(1L, UserRole.ADMIN);

        assertErrorCode(() -> jwtProvider.validateRefreshToken(token), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void expiredTokenIsReportedAsExpired() {
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiration", -1L);

        String token = jwtProvider.createAccessToken(1L, UserRole.ADMIN);

        assertErrorCode(() -> jwtProvider.validateAccessToken(token), ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void tokenWithNonNumericSubjectIsRejectedAsInvalid() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject("not-a-number")
                .claim("role", UserRole.ADMIN.name())
                .claim("category", "access")
                .expiration(new Date(System.currentTimeMillis() + 600000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertErrorCode(() -> jwtProvider.getUserId(token), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void malformedTokenIsRejectedAsInvalid() {
        assertErrorCode(() -> jwtProvider.parseToken("not-a-jwt"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void tokenWithUnsupportedRoleClaimIsRejectedWhenRoleIsRead() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject("1")
                .claim("role", "MANAGER")
                .claim("category", "access")
                .expiration(new Date(System.currentTimeMillis() + 600000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertErrorCode(() -> jwtProvider.getRole(token), ErrorCode.INVALID_TOKEN);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
