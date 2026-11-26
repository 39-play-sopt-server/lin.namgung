package org.sopt.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.domain.RefreshToken;
import org.sopt.auth.exception.AuthException;
import org.sopt.auth.repository.RefreshTokenRepository;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.exception.InvalidMemberException;
import org.sopt.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthServiceTest {
    private static final String EMAIL = "lin@sopt.org";
    private static final String PASSWORD = "sopt1234!";

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원가입하면 비밀번호를 평문이 아닌 BCrypt 해시로 저장한다")
    void signupHashesPassword() {
        MemberResponse response = authService.signup(EMAIL, PASSWORD, "린");

        Member saved = memberRepository.findById(response.id()).orElseThrow();
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"));
    }

    @Test
    @DisplayName("이메일은 앞뒤 공백을 지우고 소문자로 저장한다")
    void signupNormalizesEmail() {
        MemberResponse response = authService.signup("  Lin@SOPT.org ", PASSWORD, "린");

        assertEquals(EMAIL, response.email());
    }

    @Test
    @DisplayName("이미 가입된 이메일로는 가입할 수 없다")
    void signupWithDuplicateEmail() {
        authService.signup(EMAIL, PASSWORD, "린");

        assertErrorCode(ErrorCode.DUPLICATE_EMAIL, () -> authService.signup("LIN@sopt.org", PASSWORD, "다른 린"));
    }

    @Test
    @DisplayName("이메일 형식이 아니거나 비밀번호 길이가 맞지 않으면 가입할 수 없다")
    void signupWithInvalidInput() {
        assertErrorCode(ErrorCode.INVALID_EMAIL, () -> authService.signup("lin.sopt.org", PASSWORD, "린"));
        assertErrorCode(ErrorCode.INVALID_EMAIL, () -> authService.signup(null, PASSWORD, "린"));
        assertErrorCode(ErrorCode.INVALID_PASSWORD, () -> authService.signup(EMAIL, "short", "린"));
        assertErrorCode(ErrorCode.INVALID_PASSWORD, () -> authService.signup(EMAIL, "a".repeat(65), "린"));
        assertErrorCode(ErrorCode.INVALID_PASSWORD, () -> authService.signup(EMAIL, null, "린"));
        assertThrows(InvalidMemberException.class, () -> authService.signup(EMAIL, PASSWORD, " "));
    }

    @Test
    @DisplayName("로그인하면 회원 id가 담긴 Access Token을 발급한다")
    void login() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();

        String accessToken = authService.login(EMAIL, PASSWORD).accessToken();

        assertEquals(memberId, jwtProvider.getMemberId(accessToken));
    }

    @Test
    @DisplayName("없는 이메일과 틀린 비밀번호는 같은 에러로 응답한다")
    void loginFailsWithSameError() {
        authService.signup(EMAIL, PASSWORD, "린");

        assertErrorCode(ErrorCode.LOGIN_FAILED, () -> authService.login("none@sopt.org", PASSWORD));
        assertErrorCode(ErrorCode.LOGIN_FAILED, () -> authService.login(EMAIL, "wrong-password"));
        assertErrorCode(ErrorCode.LOGIN_FAILED, () -> authService.login(null, null));
    }

    @Test
    @DisplayName("탈퇴한 회원은 로그인할 수 없다")
    void loginWithWithdrawnMember() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();
        memberRepository.findById(memberId).orElseThrow().delete();
        memberRepository.flush();

        assertErrorCode(ErrorCode.LOGIN_FAILED, () -> authService.login(EMAIL, PASSWORD));
    }

    @Test
    @DisplayName("로그인하면 Refresh Token을 발급하고 DB에는 원문이 아닌 해시만 저장한다")
    void loginStoresHashedRefreshToken() {
        authService.signup(EMAIL, PASSWORD, "린");

        AuthTokens tokens = authService.login(EMAIL, PASSWORD);

        assertTrue(refreshTokenRepository.findByTokenHash(tokens.refreshToken()).isEmpty());
        assertTrue(refreshTokenRepository.findByTokenHash(RefreshToken.hash(tokens.refreshToken())).isPresent());
    }

    @Test
    @DisplayName("Refresh Token으로 재발급하면 새 토큰 쌍을 주고, 사용한 토큰은 다시 쓸 수 없다")
    void reissueRotatesRefreshToken() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();
        AuthTokens first = authService.login(EMAIL, PASSWORD);

        AuthTokens second = authService.reissue(first.refreshToken());

        assertEquals(memberId, jwtProvider.getMemberId(second.accessToken()));
        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue(first.refreshToken()));
        assertEquals(memberId, jwtProvider.getMemberId(authService.reissue(second.refreshToken()).accessToken()));
    }

    @Test
    @DisplayName("없거나 모르는 Refresh Token으로는 재발급할 수 없다")
    void reissueWithUnknownToken() {
        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue(null));
        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue(" "));
        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue("unknown-token"));
    }

    @Test
    @DisplayName("만료된 Refresh Token으로는 재발급할 수 없다")
    void reissueWithExpiredToken() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();
        refreshTokenRepository.save(RefreshToken.issue(memberId, "expired-token", LocalDateTime.now().minusMinutes(1)));

        assertErrorCode(ErrorCode.EXPIRED_REFRESH_TOKEN, () -> authService.reissue("expired-token"));
    }

    @Test
    @DisplayName("탈퇴한 회원의 Refresh Token으로는 재발급할 수 없다")
    void reissueAfterWithdraw() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();
        refreshTokenRepository.save(RefreshToken.issue(memberId, "left-token", LocalDateTime.now().plusDays(1)));
        memberRepository.findById(memberId).orElseThrow().delete();
        memberRepository.flush();

        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue("left-token"));
    }

    @Test
    @DisplayName("로그아웃하면 해당 Refresh Token은 더 이상 재발급에 쓸 수 없고, 반복 호출해도 에러가 나지 않는다")
    void logout() {
        authService.signup(EMAIL, PASSWORD, "린");
        AuthTokens tokens = authService.login(EMAIL, PASSWORD);

        authService.logout(tokens.refreshToken());
        authService.logout(tokens.refreshToken());
        authService.logout(null);

        assertErrorCode(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.reissue(tokens.refreshToken()));
    }

    @Test
    @DisplayName("여러 기기에서 로그인하면 기기마다 Refresh Token이 따로 저장된다")
    void loginFromMultipleDevices() {
        Long memberId = authService.signup(EMAIL, PASSWORD, "린").id();

        AuthTokens phone = authService.login(EMAIL, PASSWORD);
        AuthTokens laptop = authService.login(EMAIL, PASSWORD);
        authService.logout(phone.refreshToken());

        assertEquals(1, refreshTokenRepository.countByMemberId(memberId));
        assertEquals(memberId, jwtProvider.getMemberId(authService.reissue(laptop.refreshToken()).accessToken()));
    }

    private void assertErrorCode(ErrorCode expected, Runnable action) {
        AuthException e = assertThrows(AuthException.class, action::run);
        assertEquals(expected, e.getErrorCode());
    }
}
