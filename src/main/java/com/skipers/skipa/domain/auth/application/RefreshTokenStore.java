package com.skipers.skipa.domain.auth.application;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(Long userId, String refreshToken, Duration ttl);

    boolean matches(Long userId, String refreshToken);

    void delete(Long userId);
}
