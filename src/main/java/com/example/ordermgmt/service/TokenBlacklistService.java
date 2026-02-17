package com.example.ordermgmt.service;

public interface TokenBlacklistService {
    void blacklistToken(String token);

    boolean isBlacklisted(String token);
}
