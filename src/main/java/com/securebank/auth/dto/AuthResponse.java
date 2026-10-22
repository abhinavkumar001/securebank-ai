package com.securebank.auth.dto;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, long expiresInMinutes,
                           Long userId, String name, String role, Instant previousLoginAt) {}
