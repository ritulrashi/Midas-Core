package com.midas.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final String userId;
    private final String role;

    public static TokenResponse of(String token, long expiresInMs, String userId, String role) {
        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresInMs / 1000)
                .userId(userId)
                .role(role)
                .build();
    }
}
