package com.minsu.mockstocklive.auth.service;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.dto.LoginRequest;
import com.minsu.mockstocklive.auth.dto.LoginResponse;
import com.minsu.mockstocklive.auth.dto.SignupRequest;
import com.minsu.mockstocklive.auth.dto.SignupResponse;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.exception.DuplicateResourceException;
import com.minsu.mockstocklive.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final BigDecimal INITIAL_CASH_BALANCE = new BigDecimal("10000000.00");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedNickname = request.nickname().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered");
        }

        if (userRepository.existsByNickname(normalizedNickname)) {
            throw new DuplicateResourceException("Nickname already registered");
        }

        User savedUser = userRepository.save(User.create(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                normalizedNickname,
                INITIAL_CASH_BALANCE
        ));

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname(),
                savedUser.getCashBalance(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCashBalance(),
                "Login successful",
                LocalDateTime.now()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
