package com.gpp.jwt_authentication_service.controller;

import com.gpp.jwt_authentication_service.dto.TokenVerificationResponse;
import com.gpp.jwt_authentication_service.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TokenVerificationController {

    private final JwtService jwtService;

    @GetMapping("/verify-token")
    public ResponseEntity<TokenVerificationResponse> verifyToken(@RequestParam("token") String token) {
        try {
            Claims claims = jwtService.extractAllClaims(token);
            
            if (jwtService.isTokenExpired(token)) {
                return ResponseEntity.ok(TokenVerificationResponse.builder()
                        .valid(false)
                        .reason("Token has expired")
                        .build());
            }

            Map<String, Object> claimsMap = new HashMap<>(claims);
            // Ensure expiration is mapped properly to a number as requested
            claimsMap.put("exp", claims.getExpiration().getTime() / 1000); // Unix timestamp
            claimsMap.put("iat", claims.getIssuedAt().getTime() / 1000);   // Unix timestamp

            return ResponseEntity.ok(TokenVerificationResponse.builder()
                    .valid(true)
                    .claims(claimsMap)
                    .build());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.ok(TokenVerificationResponse.builder()
                    .valid(false)
                    .reason("Token has expired")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(TokenVerificationResponse.builder()
                    .valid(false)
                    .reason("Invalid signature or malformed token")
                    .build());
        }
    }
}
