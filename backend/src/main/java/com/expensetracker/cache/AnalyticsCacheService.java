package com.expensetracker.cache;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Thin wrapper around RedisTemplate implementing the cache-aside pattern for analytics endpoints.
 *
 * Flow: check Redis -> HIT returns cached value -> MISS computes via supplier, stores in Redis, returns.
 * If Redis is down for any reason, we log and fall straight through to the supplier (MySQL calculation)
 * instead of failing the request — Redis is an optimization, not a hard dependency.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheService.class);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache HIT for key={}", key);
                return type.cast(cached);
            }
            log.debug("Cache MISS for key={}", key);
        } catch (Exception ex) {
            log.warn("Redis unavailable on read (key={}), falling back to direct calculation: {}", key, ex.getMessage());
        }

        T computed = supplier.get();

        try {
            redisTemplate.opsForValue().set(key, computed, DEFAULT_TTL);
        } catch (Exception ex) {
            log.warn("Redis unavailable on write (key={}), skipping cache store: {}", key, ex.getMessage());
        }

        return computed;
    }

    public void evict(String keyPrefix) {
        try {
            var keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} cache keys matching {}*", keys.size(), keyPrefix);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable during cache eviction (prefix={}): {}", keyPrefix, ex.getMessage());
        }
    }
}
