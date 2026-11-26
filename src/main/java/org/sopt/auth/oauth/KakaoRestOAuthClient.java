package org.sopt.auth.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.auth.exception.AuthException;
import org.sopt.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoRestOAuthClient implements KakaoOAuthClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoRestOAuthClient.class);

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoRestOAuthClient(
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.client-secret}") String clientSecret,
            @Value("${oauth.kakao.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public KakaoUserInfo getUserInfo(String authorizationCode) {
        if (clientId == null || clientId.isBlank()) {
            log.error("카카오 client-id가 설정되지 않았습니다. 환경 변수 KAKAO_CLIENT_ID를 확인해주세요.");
            throw new AuthException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        try {
            String kakaoAccessToken = requestAccessToken(authorizationCode);
            KakaoUserResponse user = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
            if (user == null || user.id() == null) {
                throw new AuthException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
            return new KakaoUserInfo(String.valueOf(user.id()), user.nickname());
        } catch (RestClientException e) {
            log.warn("카카오 로그인 요청에 실패했습니다: {}", e.getMessage());
            throw new AuthException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    private String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new AuthException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return response.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        String nickname() {
            if (kakaoAccount == null || kakaoAccount.profile() == null) {
                return null;
            }
            return kakaoAccount.profile().nickname();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {
    }
}
