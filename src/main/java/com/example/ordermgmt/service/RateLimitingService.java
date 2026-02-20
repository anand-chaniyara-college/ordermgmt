package com.example.ordermgmt.service;

public interface RateLimitingService {
    /**
     * Checks if a request is allowed based on the rate limit key.
     *
     * @param key             The unique key for the client (e.g., IP address).
     * @param maxRequests     Maximum number of requests allowed in the window.
     * @param windowInSeconds The time window in seconds.
     * @return true if allowed, false if limit exceeded.
     */
    boolean allowRequest(String key, long maxRequests, long windowInSeconds);
}
