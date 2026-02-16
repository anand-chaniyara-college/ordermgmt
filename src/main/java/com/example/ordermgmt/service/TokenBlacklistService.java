package com.example.ordermgmt.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private final long jwtExpirationMs;
    private final Cache<String, Boolean> blacklistCache;

    public TokenBlacklistService(@Value("${jwt.expiration}") long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
        this.blacklistCache = Caffeine.newBuilder()
                .expireAfterWrite(jwtExpirationMs, TimeUnit.MILLISECONDS)
                .maximumSize(10_000)
                .build();
    }

    public void blacklistToken(String token) {
        blacklistCache.put(token, true);
    }

    public boolean isBlacklisted(String token) {
        return blacklistCache.getIfPresent(token) != null;
    }
}
