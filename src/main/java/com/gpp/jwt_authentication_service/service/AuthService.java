package com.gpp.jwt_authentication_service.service;

import com.gpp.jwt_authentication_service.dto.AuthResponse;
import com.gpp.jwt_authentication_service.dto.LoginRequest;
import com.gpp.jwt_authentication_service.dto.RegisterRequest;
import com.gpp.jwt_authentication_service.dto.TokenRefreshRequest;
import com.gpp.jwt_authentication_service.model.RefreshToken;
import com.gpp.jwt_authentication_service.model.User;
import com.gpp.jwt_authentication_service.repository.RefreshTokenRepository;
import com.gpp.jwt_authentication_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()) ||
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "message", "User registered successfully"
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        String jwtToken = jwtService.generateToken(user.getUsername(), Collections.singletonList("user"));
        String refreshToken = createRefreshToken(user).getToken();

        return AuthResponse.builder()
                .token_type("Bearer")
                .access_token(jwtToken)
                .expires_in(jwtService.getExpirationTime() / 1000)
                .refresh_token(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenRepository.findByToken(request.getRefresh_token())
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtService.generateToken(user.getUsername(), Collections.singletonList("user"));
                    return AuthResponse.builder()
                            .token_type("Bearer")
                            .access_token(token)
                            .expires_in(jwtService.getExpirationTime() / 1000)
                            // Optionally rotate refresh token, but per requirements we just return new access token
                            .build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        refreshTokenRepository.findByToken(request.getRefresh_token())
                .ifPresent(token -> refreshTokenRepository.delete(token));
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
