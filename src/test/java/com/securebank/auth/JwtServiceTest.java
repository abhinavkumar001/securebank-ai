package com.securebank.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit test: no Spring context, no database. */
class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("unit-test-secret-key-that-is-long-enough-123", 60);

    @Test
    void generatedTokenCanBeParsedBack() {
        User user = new User("Abhinav", "abhinav@test.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 42L);

        Optional<Claims> claims = jwtService.parse(jwtService.generateToken(user));

        assertTrue(claims.isPresent());
        assertEquals("42", claims.get().getSubject());
        assertEquals("abhinav@test.com", claims.get().get("email", String.class));
        assertEquals("CUSTOMER", claims.get().get("role", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        User user = new User("Abhinav", "abhinav@test.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 42L);
        String token = jwtService.generateToken(user);

        // Change one character in the signature part
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertTrue(jwtService.parse(tampered).isEmpty());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        User user = new User("Abhinav", "abhinav@test.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 1L);
        JwtService attacker = new JwtService("a-completely-different-secret-key-9876543210", 60);

        assertTrue(jwtService.parse(attacker.generateToken(user)).isEmpty());
    }

    @Test
    void shortSecretIsRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60));
    }
}
