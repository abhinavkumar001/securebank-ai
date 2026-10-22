package com.securebank.auth;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around BCrypt. BCrypt adds a random salt to every hash
 * and is deliberately slow, which makes brute-forcing stolen hashes expensive.
 */
@Component
public class PasswordHasher {

    private static final int COST = 10;

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public boolean matches(String rawPassword, String hash) {
        return BCrypt.checkpw(rawPassword, hash);
    }
}
