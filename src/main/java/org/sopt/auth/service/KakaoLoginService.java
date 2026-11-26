package org.sopt.auth.service;

import org.sopt.auth.exception.AuthException;
import org.sopt.auth.oauth.KakaoOAuthClient;
import org.sopt.auth.oauth.KakaoUserInfo;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.member.domain.SocialProvider;
import org.sopt.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoLoginService {
    private static final String DEFAULT_NAME = "카카오 회원";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final TokenIssuer tokenIssuer;

    public KakaoLoginService(
            KakaoOAuthClient kakaoOAuthClient,
            MemberRepository memberRepository,
            TokenIssuer tokenIssuer
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.memberRepository = memberRepository;
        this.tokenIssuer = tokenIssuer;
    }

    public String getAuthorizationUrl() {
        return kakaoOAuthClient.getAuthorizationUrl();
    }

    /**
     * 인가 코드로 카카오 회원 정보를 받아, 처음이면 가입시키고 우리 서비스의 토큰을 발급한다.
     * 카카오 토큰은 회원 확인에만 쓰고 저장하거나 클라이언트에 넘기지 않는다.
     */
    @Transactional
    public AuthTokens login(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new AuthException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(authorizationCode);
        Member member = memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, userInfo.id())
                .orElseGet(() -> memberRepository.save(
                        Member.createWithSocial(nameOf(userInfo), SocialProvider.KAKAO, userInfo.id())));
        if (member.isDeleted()) {
            throw new AuthException(ErrorCode.WITHDRAWN_MEMBER);
        }
        return tokenIssuer.issue(member.getId());
    }

    private String nameOf(KakaoUserInfo userInfo) {
        String nickname = userInfo.nickname();
        return nickname == null || nickname.isBlank() ? DEFAULT_NAME : nickname;
    }
}
