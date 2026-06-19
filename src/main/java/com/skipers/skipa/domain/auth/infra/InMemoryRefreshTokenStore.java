package com.skipers.skipa.domain.auth.infra;

import com.skipers.skipa.domain.auth.application.RefreshTokenStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local", "local-postgres", "test"})
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<Long, Entry> tokens = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String refreshToken, Duration ttl) {
        tokens.put(userId, new Entry(refreshToken, Instant.now().plus(ttl)));
    }

    @Override
    public boolean matches(Long userId, String refreshToken) {
        Entry entry = tokens.get(userId);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            tokens.remove(userId);
            return false;
        }
        return entry.token().equals(refreshToken);
    }

    @Override
    public void delete(Long userId) {
        tokens.remove(userId);
    }

    private record Entry(String token, Instant expiresAt) {

        private boolean isExpired() {
            return !expiresAt.isAfter(Instant.now());
        }
    }
}
