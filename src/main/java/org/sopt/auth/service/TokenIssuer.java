package org.sopt.auth.service;

import org.sopt.auth.domain.RefreshToken;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Access Token(JWT)과 Refresh Token(무작위 문자열)을 함께 발급한다.
 * Refresh Token은 원문을 클라이언트에만 주고, DB에는 SHA-256 해시만 저장한다.
 */
@Component
public class TokenIssuer {
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpiration;

    public TokenIssuer(
            JwtProvider jwtProvider,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Transactional
    public AuthTokens issue(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredByMemberId(memberId, now);

        String rawRefreshToken = generateRefreshToken();
        LocalDateTime expiresAt = now.plus(Duration.ofMillis(refreshTokenExpiration));
        refreshTokenRepository.save(RefreshToken.issue(memberId, rawRefreshToken, expiresAt));

        return new AuthTokens(jwtProvider.createAccessToken(memberId), rawRefreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
