package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.service.RateLimitingService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private final StringRedisTemplate redisTemplate;
    public RateLimitingServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean allowRequest(String key, String endpoint, long maxRequests, long windowInSeconds) {
        String redisKey = RateLimitingService.RATE_LIMIT_PREFIX + endpoint + ":" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowInSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= maxRequests;
    }
}
