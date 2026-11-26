package org.sopt.auth.controller;

import org.sopt.auth.dto.request.KakaoLoginRequest;
import org.sopt.auth.dto.request.LoginRequest;
import org.sopt.auth.dto.request.SignupRequest;
import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.auth.service.AuthService;
import org.sopt.auth.service.AuthTokens;
import org.sopt.auth.service.KakaoLoginService;
import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/auth")
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final KakaoLoginService kakaoLoginService;
    private final RefreshTokenCookieFactory cookieFactory;

    public AuthController(
            AuthService authService,
            KakaoLoginService kakaoLoginService,
            RefreshTokenCookieFactory cookieFactory
    ) {
        this.authService = authService;
        this.kakaoLoginService = kakaoLoginService;
        this.cookieFactory = cookieFactory;
    }

    @Override
    @PostMapping(path = "/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(@RequestBody SignupRequest request) {
        MemberResponse response = authService.signup(request.email(), request.password(), request.name());
        return ApiResponse.toResponseEntity(SuccessCode.MEMBER_CREATED, response);
    }

    @Override
    @PostMapping(path = "/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.email(), request.password());
        return withTokens(SuccessCode.LOGIN_SUCCESS, tokens);
    }

    @Override
    @PostMapping(path = "/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken
    ) {
        AuthTokens tokens = authService.reissue(refreshToken);
        return withTokens(SuccessCode.TOKEN_REISSUED, tokens);
    }

    @Override
    @PostMapping(path = "/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.status(SuccessCode.LOGOUT_SUCCESS.getStatus())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expire().toString())
                .body(ApiResponse.of(SuccessCode.LOGOUT_SUCCESS));
    }

    @Override
    @GetMapping(path = "/kakao")
    public ResponseEntity<Void> redirectToKakao() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, kakaoLoginService.getAuthorizationUrl())
                .build();
    }

    @Override
    @GetMapping(path = "/kakao/callback")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoCallback(
            @RequestParam(name = "code", required = false) String code
    ) {
        AuthTokens tokens = kakaoLoginService.login(code);
        return withTokens(SuccessCode.LOGIN_SUCCESS, tokens);
    }

    @Override
    @PostMapping(path = "/kakao/login")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        AuthTokens tokens = kakaoLoginService.login(request.code());
        return withTokens(SuccessCode.LOGIN_SUCCESS, tokens);
    }

    private ResponseEntity<ApiResponse<TokenResponse>> withTokens(SuccessCode successCode, AuthTokens tokens) {
        return ResponseEntity.status(successCode.getStatus())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(tokens.refreshToken()).toString())
                .body(ApiResponse.of(successCode, new TokenResponse(tokens.accessToken())));
    }
}
