package com.tibame;

import com.tibame.common.crypto.token.TokenService;
import com.tibame.common.crypto.token.impl.JwtTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenServiceTest {

    private JwtTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenServiceImpl(
                "SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026",
                3600000
        );
    }

    @Test
    void testGenerateAndValidateToken() {
        Long userId = 1001L;
        String username = "testuser";

        String token = tokenService.generateToken(userId, username);
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));

        assertEquals(userId, tokenService.getUserIdFromToken(token));
        assertEquals(username, tokenService.getUsernameFromToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(tokenService.validateToken("invalid.jwt.token"));
    }
}
