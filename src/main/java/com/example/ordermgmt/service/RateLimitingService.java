package com.example.ordermgmt.service;

public interface RateLimitingService {
    String RATE_LIMIT_PREFIX = "rate_limit:";
    String ENDPOINT_REGISTER = "register";
    String ENDPOINT_FORGOT_PASSWORD = "forgot-password";

    /**
     * Checks if a request is allowed based on the rate limit key.
     *
     * @param key             The unique key for the client (e.g., IP address).
     * @param endpoint        The endpoint being rate limited (e.g., "register", "forgot-password").
     * @param maxRequests     Maximum number of requests allowed in the window.
     * @param windowInSeconds The time window in seconds.
     * @return true if allowed, false if limit exceeded.
     */
    boolean allowRequest(String key, String endpoint, long maxRequests, long windowInSeconds);
}
