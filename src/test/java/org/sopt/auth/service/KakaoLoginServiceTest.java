package org.sopt.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.exception.AuthException;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.auth.oauth.KakaoOAuthClient;
import org.sopt.auth.oauth.KakaoUserInfo;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.member.domain.SocialProvider;
import org.sopt.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class KakaoLoginServiceTest {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JwtProvider jwtProvider;

    private FakeKakaoOAuthClient kakaoClient;
    private KakaoLoginService kakaoLoginService;

    @BeforeEach
    void setUp() {
        kakaoClient = new FakeKakaoOAuthClient();
        kakaoLoginService = new KakaoLoginService(kakaoClient, memberRepository, tokenIssuer);
    }

    @Test
    @DisplayName("처음 카카오로 로그인하면 카카오 닉네임으로 가입시키고 토큰을 발급한다")
    void firstLoginSignsUp() {
        kakaoClient.user = new KakaoUserInfo("1001", "카카오린");

        AuthTokens tokens = kakaoLoginService.login("code");

        Member member = memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "1001").orElseThrow();
        assertEquals("카카오린", member.getName());
        assertEquals(member.getId(), jwtProvider.getMemberId(tokens.accessToken()));
    }

    @Test
    @DisplayName("이미 가입한 카카오 회원은 새로 가입시키지 않고 같은 회원으로 로그인한다")
    void secondLoginUsesSameMember() {
        kakaoClient.user = new KakaoUserInfo("1001", "카카오린");
        Long firstId = jwtProvider.getMemberId(kakaoLoginService.login("code-1").accessToken());

        Long secondId = jwtProvider.getMemberId(kakaoLoginService.login("code-2").accessToken());

        assertEquals(firstId, secondId);
    }

    @Test
    @DisplayName("닉네임 동의를 하지 않아 닉네임이 없으면 기본 이름으로 가입시킨다")
    void loginWithoutNickname() {
        kakaoClient.user = new KakaoUserInfo("1002", null);

        kakaoLoginService.login("code");

        Member member = memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "1002").orElseThrow();
        assertEquals("카카오 회원", member.getName());
    }

    @Test
    @DisplayName("탈퇴한 카카오 회원은 로그인할 수 없다")
    void withdrawnMemberCannotLogin() {
        Member member = memberRepository.save(Member.createWithSocial("린", SocialProvider.KAKAO, "1003"));
        member.delete();
        memberRepository.flush();
        kakaoClient.user = new KakaoUserInfo("1003", "린");

        AuthException e = assertThrows(AuthException.class, () -> kakaoLoginService.login("code"));
        assertEquals(ErrorCode.WITHDRAWN_MEMBER, e.getErrorCode());
    }

    @Test
    @DisplayName("인가 코드가 없으면 카카오에 요청하지 않고 실패한다")
    void loginWithoutCode() {
        AuthException e = assertThrows(AuthException.class, () -> kakaoLoginService.login(" "));
        assertEquals(ErrorCode.SOCIAL_LOGIN_FAILED, e.getErrorCode());
        assertEquals(0, kakaoClient.calls);
    }

    private static class FakeKakaoOAuthClient implements KakaoOAuthClient {
        private KakaoUserInfo user;
        private int calls;

        @Override
        public String getAuthorizationUrl() {
            return "https://kauth.kakao.com/oauth/authorize";
        }

        @Override
        public KakaoUserInfo getUserInfo(String authorizationCode) {
            calls++;
            return user;
        }
    }
}
