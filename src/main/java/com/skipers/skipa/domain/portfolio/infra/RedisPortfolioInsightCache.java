package com.skipers.skipa.domain.portfolio.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCache;
import com.skipers.skipa.domain.portfolio.exception.PortfolioException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class RedisPortfolioInsightCache implements PortfolioInsightCache {

    private static final String CACHE_KEY = "portfolio:ai-insights";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<List<String>> get() {
        String value = redisTemplate.opsForValue().get(CACHE_KEY);
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, new TypeReference<>() {
            }));
        } catch (JsonProcessingException e) {
            throw new PortfolioException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void put(List<String> insights, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(insights), ttl);
        } catch (JsonProcessingException e) {
            throw new PortfolioException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void evict() {
        redisTemplate.delete(CACHE_KEY);
    }
}
