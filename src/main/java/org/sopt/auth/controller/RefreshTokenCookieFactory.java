package org.sopt.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token을 담는 쿠키를 만든다.
 * - HttpOnly: JavaScript(document.cookie)로 읽을 수 없어 XSS로 탈취되지 않는다.
 * - Secure: HTTPS 요청에만 실린다.
 * - Path=/api/v1/auth: 재발급, 로그아웃 요청에만 실려서 다른 API 요청마다 노출되지 않는다.
 */
@Component
public class RefreshTokenCookieFactory {
    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final Duration maxAge;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieFactory(
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.refresh-cookie.secure}") boolean secure,
            @Value("${jwt.refresh-cookie.same-site}") String sameSite
    ) {
        this.maxAge = Duration.ofMillis(refreshTokenExpiration);
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    public ResponseCookie expire() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }
}
