package org.sopt.auth.service;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
