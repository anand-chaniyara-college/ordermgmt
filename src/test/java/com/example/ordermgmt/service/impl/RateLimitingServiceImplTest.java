package com.example.ordermgmt.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingServiceImpl rateLimitingService;

    private static final String KEY = "test-key";
    private static final long MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;
    private static final String TEST_PREFIX = com.example.ordermgmt.service.RateLimitingService.RATE_LIMIT_PREFIX + com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER + ":";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void allowRequest_FirstRequest_ReturnsTrue() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(1L);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS);

        assertTrue(result);
        verify(redisTemplate).expire(TEST_PREFIX + KEY, WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void allowRequest_WithinLimit_ReturnsTrue() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(3L);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS);

        assertTrue(result);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void allowRequest_AtLimit_ReturnsTrue() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(MAX_REQUESTS);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS);

        assertTrue(result);
    }

    @Test
    void allowRequest_ExceedsLimit_ReturnsFalse() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(MAX_REQUESTS + 1);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS);

        assertFalse(result);
    }

    @Test
    void allowRequest_WithNullIncrement_ReturnsFalse() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(null);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS);

        assertFalse(result);
    }

    @Test
    void allowRequest_MultipleKeys_WorkIndependently() {
        String key1 = "key1";
        String key2 = "key2";

        when(valueOperations.increment(TEST_PREFIX + key1)).thenReturn(1L);
        when(valueOperations.increment(TEST_PREFIX + key2)).thenReturn(1L);

        assertTrue(rateLimitingService.allowRequest(key1, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS));
        assertTrue(rateLimitingService.allowRequest(key2, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, WINDOW_SECONDS));

        verify(redisTemplate).expire(TEST_PREFIX + key1, WINDOW_SECONDS, TimeUnit.SECONDS);
        verify(redisTemplate).expire(TEST_PREFIX + key2, WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void allowRequest_WithDifferentWindows_RespectsEachWindow() {
        long smallWindow = 30;
        long largeWindow = 120;

        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(1L);

        rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, smallWindow);
        verify(redisTemplate).expire(TEST_PREFIX + KEY, smallWindow, TimeUnit.SECONDS);

        rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, MAX_REQUESTS, largeWindow);
        verify(redisTemplate).expire(TEST_PREFIX + KEY, largeWindow, TimeUnit.SECONDS);
    }

    @Test
    void allowRequest_WithZeroMaxRequests_AlwaysReturnsFalse() {
        when(valueOperations.increment(TEST_PREFIX + KEY)).thenReturn(1L);

        boolean result = rateLimitingService.allowRequest(KEY, com.example.ordermgmt.service.RateLimitingService.ENDPOINT_REGISTER, 0, WINDOW_SECONDS);

        assertFalse(result);
    }
}
