package org.sopt.auth.oauth;

/**
 * 카카오 서버와 통신하는 부분을 분리한 인터페이스.
 * 테스트에서는 실제 카카오 서버 대신 가짜 구현을 넣는다.
 */
public interface KakaoOAuthClient {

    String getAuthorizationUrl();

    KakaoUserInfo getUserInfo(String authorizationCode);
}
