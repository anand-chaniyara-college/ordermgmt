package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.service.TokenBlacklistService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);
    private static final String BLACKLIST_PREFIX = "blacklist:access:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistServiceImpl(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklistToken(String token) {
        String key = BLACKLIST_PREFIX + token;
        long remainingTtlMs = jwtUtil.getRemainingValidityMillis(token);

        if (remainingTtlMs <= 0) {
            logger.debug("Skipping blacklist for expired/invalid token: {}", messageDigest(token));
            return;
        }

        redisTemplate.opsForValue().set(key, "true", remainingTtlMs, TimeUnit.MILLISECONDS);
        logger.debug("Token blacklisted: {}", messageDigest(token));
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (isBlacklisted) {
            logger.debug("Token check: BLACKLISTED ({})", messageDigest(token));
        }
        return isBlacklisted;
    }

    private String messageDigest(String token) {
        return token.length() > 10 ? token.substring(0, 5) + "..." + token.substring(token.length() - 5)
                : "short-token";
    }
}
