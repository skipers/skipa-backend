package com.skipers.skipa.global.security;

import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String CATEGORY_CLAIM = "category";

    private static final String ACCESS_CATEGORY = "access";
    private static final String REFRESH_CATEGORY = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.access}")
    private long accessTokenExpiration;

    @Value("${jwt.expiration.refresh}")
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    public record RefreshTokenResult(String token, String jti) {
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, UserRole role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .claim(CATEGORY_CLAIM, ACCESS_CATEGORY)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public RefreshTokenResult createRefreshToken(Long userId) {
        String jti = UUID.randomUUID().toString();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .claim(CATEGORY_CLAIM, REFRESH_CATEGORY)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        return new RefreshTokenResult(token, jti);
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AuthException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void validateAccessToken(String token) {
        Claims claims = parseToken(token);
        String category = claims.get(CATEGORY_CLAIM, String.class);

        if (!ACCESS_CATEGORY.equals(category)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void validateRefreshToken(String token) {
        Claims claims = parseToken(token);
        String category = claims.get(CATEGORY_CLAIM, String.class);

        if (!REFRESH_CATEGORY.equals(category)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token) {
        try {
            return Long.parseLong(parseToken(token).getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    public UserRole getRole(String token) {
        String role = parseToken(token).get(ROLE_CLAIM, String.class);

        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String getJti(String token) {
        return parseToken(token).getId();
    }
}
