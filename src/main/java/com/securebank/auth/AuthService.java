package com.securebank.auth;

import com.securebank.audit.AuditService;
import com.securebank.auth.dto.AuthResponse;
import com.securebank.auth.dto.LoginRequest;
import com.securebank.auth.dto.RegisterRequest;
import com.securebank.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        User user = new User(request.name().trim(), email, passwordHasher.hash(request.password()), Role.CUSTOMER);
        user.recordLogin(Instant.now());
        user = userRepository.save(user);

        auditService.log(user.getId(), "REGISTER", "User", user.getId(), "New customer registered");
        return toResponse(user, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Same message for "no such user" and "wrong password",
        // so attackers can't find out which emails are registered.
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordHasher.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        Instant previous = user.recordLogin(Instant.now());
        auditService.log(user.getId(), "LOGIN", "User", user.getId(), "Successful login");
        return toResponse(user, previous);
    }

    private AuthResponse toResponse(User user, Instant previousLogin) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationMinutes(), user.getId(), user.getName(), user.getRole().name(),
                previousLogin);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
