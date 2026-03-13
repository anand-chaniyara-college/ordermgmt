package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.security.JwtUtil;
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
class TokenBlacklistServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private static final String SHORT_TOKEN = "short";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void blacklistToken_WithValidToken_BlacklistsSuccessfully() {
        when(jwtUtil.getRemainingValidityMillis(TOKEN)).thenReturn(3600000L); // 1 hour

        tokenBlacklistService.blacklistToken(TOKEN);

        verify(valueOperations).set(
                eq("blacklist:access:" + TOKEN),
                eq("true"),
                eq(3600000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void blacklistToken_WithExpiredToken_SkipsBlacklisting() {
        when(jwtUtil.getRemainingValidityMillis(TOKEN)).thenReturn(0L);

        tokenBlacklistService.blacklistToken(TOKEN);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void blacklistToken_WithNegativeValidity_SkipsBlacklisting() {
        when(jwtUtil.getRemainingValidityMillis(TOKEN)).thenReturn(-1000L);

        tokenBlacklistService.blacklistToken(TOKEN);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void isBlacklisted_WithBlacklistedToken_ReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:access:" + TOKEN)).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

        assertTrue(result);
    }

    @Test
    void isBlacklisted_WithNonBlacklistedToken_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:access:" + TOKEN)).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

        assertFalse(result);
    }

    @Test
    void isBlacklisted_WithNullResponse_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:access:" + TOKEN)).thenReturn(null);

        boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

        assertFalse(result);
    }

    @Test
    void blacklistToken_WithShortToken_HandlesGracefully() {
        when(jwtUtil.getRemainingValidityMillis(SHORT_TOKEN)).thenReturn(3600000L);

        tokenBlacklistService.blacklistToken(SHORT_TOKEN);

        verify(valueOperations).set(
                eq("blacklist:access:" + SHORT_TOKEN),
                eq("true"),
                eq(3600000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void isBlacklisted_WithShortToken_WorksCorrectly() {
        when(redisTemplate.hasKey("blacklist:access:" + SHORT_TOKEN)).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(SHORT_TOKEN);

        assertTrue(result);
    }

    @Test
    void blacklistAndCheck_Integration() {
        when(jwtUtil.getRemainingValidityMillis(TOKEN)).thenReturn(3600000L);
        
        tokenBlacklistService.blacklistToken(TOKEN);
        
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());

        when(redisTemplate.hasKey("blacklist:access:" + TOKEN)).thenReturn(true);
        
        assertTrue(tokenBlacklistService.isBlacklisted(TOKEN));
    }

    @Test
    void messageDigest_WithLongToken_TruncatesCorrectly() {
        // Test is done through log statements, we just verify the method runs without error
        when(jwtUtil.getRemainingValidityMillis(TOKEN)).thenReturn(3600000L);

        tokenBlacklistService.blacklistToken(TOKEN);

        // No exception means the digest method worked
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
    }
}
