package org.sopt.auth.controller;

import org.sopt.auth.dto.request.LoginRequest;
import org.sopt.auth.dto.request.SignupRequest;
import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.auth.service.AuthService;
import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/auth")
public class AuthController implements AuthApi {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
        TokenResponse response = authService.login(request.email(), request.password());
        return ApiResponse.toResponseEntity(SuccessCode.LOGIN_SUCCESS, response);
    }
}
