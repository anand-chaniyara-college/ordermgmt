package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.service.RateLimitingService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:register:";

    public RateLimitingServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean allowRequest(String key, long maxRequests, long windowInSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowInSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= maxRequests;
    }
}
