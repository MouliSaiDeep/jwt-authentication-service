package com.gpp.jwt_authentication_service.controller;

import com.gpp.jwt_authentication_service.dto.AuthResponse;
import com.gpp.jwt_authentication_service.dto.LoginRequest;
import com.gpp.jwt_authentication_service.dto.RegisterRequest;
import com.gpp.jwt_authentication_service.dto.TokenRefreshRequest;
import com.gpp.jwt_authentication_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
