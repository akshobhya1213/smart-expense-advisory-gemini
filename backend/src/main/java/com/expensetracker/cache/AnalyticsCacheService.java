package com.expensetracker.cache;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Optional Redis cache for analytics. Redis is an optimization, never a hard dependency.
 * It is disabled by default so deployments without a Redis instance do not generate
 * connection failures or waste request time trying to reach localhost:6379.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheService.class);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.redis.enabled:false}")
    private boolean redisEnabled;

    public <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier) {
        if (!redisEnabled) {
            return supplier.get();
        }

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache HIT for key={}", key);
                return type.cast(cached);
            }
            log.debug("Cache MISS for key={}", key);
        } catch (Exception ex) {
            log.debug("Redis unavailable on read (key={}): {}", key, ex.getMessage());
        }

        T computed = supplier.get();

        try {
            redisTemplate.opsForValue().set(key, computed, DEFAULT_TTL);
        } catch (Exception ex) {
            log.debug("Redis unavailable on write (key={}): {}", key, ex.getMessage());
        }

        return computed;
    }

    public void evict(String keyPrefix) {
        if (!redisEnabled) {
            return;
        }

        try {
            var keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} cache keys matching {}*", keys.size(), keyPrefix);
            }
        } catch (Exception ex) {
            log.debug("Redis unavailable during cache eviction (prefix={}): {}", keyPrefix, ex.getMessage());
        }
    }
}
