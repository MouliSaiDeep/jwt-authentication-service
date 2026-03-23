package com.gpp.jwt_authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refresh_token;
}
