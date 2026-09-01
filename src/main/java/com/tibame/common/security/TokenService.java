package com.tibame.common.security;

public interface TokenService {
    String generateToken(Long userId, String username);
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
    String getUsernameFromToken(String token);
}
