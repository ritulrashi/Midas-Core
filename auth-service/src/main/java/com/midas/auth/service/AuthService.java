package com.midas.auth.service;

import com.midas.auth.dto.LoginRequest;
import com.midas.auth.dto.RegisterRequest;
import com.midas.auth.dto.TokenResponse;
import com.midas.auth.model.User;
import com.midas.auth.repository.UserRepository;
import com.midas.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generate(saved.getId().toString(), saved.getRole().name());
        return TokenResponse.of(token, jwtTokenProvider.getExpirationMs(),
                saved.getId().toString(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtTokenProvider.generate(user.getId().toString(), user.getRole().name());
        return TokenResponse.of(token, jwtTokenProvider.getExpirationMs(),
                user.getId().toString(), user.getRole().name());
    }
}
