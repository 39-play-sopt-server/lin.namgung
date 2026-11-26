package org.sopt.auth.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.exception.AuthException;
import org.sopt.global.exception.ErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtProviderTest {
    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1zb3B0LTZ0aC1hc3NpZ25tZW50";
    private static final String OTHER_SECRET = "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1zaWduYXR1cmUtbWlzbWF0Y2g=";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1_800_000L);

    @Test
    @DisplayName("발급한 토큰에서 회원 id를 꺼낼 수 있다")
    void createAndParse() {
        String token = jwtProvider.createAccessToken(7L);

        assertEquals(7L, jwtProvider.getMemberId(token));
    }

    @Test
    @DisplayName("토큰의 일부를 바꾸면 서명 검증에 실패한다")
    void tamperedToken() {
        String token = jwtProvider.createAccessToken(7L);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + flipFirstChar(parts[2]);

        assertErrorCode(ErrorCode.INVALID_TOKEN, tampered);
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 받아들이지 않는다")
    void tokenSignedWithOtherKey() {
        String token = new JwtProvider(OTHER_SECRET, 1_800_000L).createAccessToken(7L);

        assertErrorCode(ErrorCode.INVALID_TOKEN, token);
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰은 만료 에러로 구분한다")
    void expiredToken() {
        String token = new JwtProvider(SECRET, -60_000L).createAccessToken(7L);

        assertErrorCode(ErrorCode.EXPIRED_TOKEN, token);
    }

    @Test
    @DisplayName("JWT 형식이 아니면 유효하지 않은 토큰이다")
    void malformedToken() {
        assertErrorCode(ErrorCode.INVALID_TOKEN, "not-a-jwt");
        assertErrorCode(ErrorCode.INVALID_TOKEN, "");
    }

    private void assertErrorCode(ErrorCode expected, String token) {
        AuthException e = assertThrows(AuthException.class, () -> jwtProvider.getMemberId(token));
        assertEquals(expected, e.getErrorCode());
    }

    private String flipFirstChar(String value) {
        char first = value.charAt(0);
        char replaced = first == 'A' ? 'B' : 'A';
        return replaced + value.substring(1);
    }
}
