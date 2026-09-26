package com.expensetracker.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsCacheServiceTest {

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    private AnalyticsCacheService cacheService() {
        return new AnalyticsCacheService(redisTemplate);
    }

    @Test
    void getOrCompute_returnsCachedValue_onCacheHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn("cached-value");

        String result = cacheService().getOrCompute("key1", String.class, () -> "computed-value");

        assertThat(result).isEqualTo("cached-value");
        verify(valueOperations, never()).set(any(), any());
    }

    @Test
    void getOrCompute_computesAndStores_onCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn(null);

        String result = cacheService().getOrCompute("key1", String.class, () -> "computed-value");

        assertThat(result).isEqualTo("computed-value");
        verify(valueOperations).set(eq("key1"), eq("computed-value"), any());
    }

    @Test
    void getOrCompute_fallsBackToSupplier_whenRedisThrowsOnRead() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        // Should not propagate the Redis exception — the whole point of the fallback.
        String result = cacheService().getOrCompute("key1", String.class, () -> "computed-value");

        assertThat(result).isEqualTo("computed-value");
    }

    @Test
    void evict_doesNotThrow_whenRedisIsDown() {
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        // Should swallow the failure, not crash the calling write operation (e.g. expense delete).
        cacheService().evict("analytics:1");
    }
}
