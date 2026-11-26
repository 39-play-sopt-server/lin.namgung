package org.sopt.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.auth.dto.request.LoginRequest;
import org.sopt.auth.dto.request.SignupRequest;
import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.global.response.ApiResponse;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "회원가입, 로그인 API")
public interface AuthApi {

    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 이름으로 가입합니다. 비밀번호는 BCrypt로 해싱해서 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이메일 형식, 비밀번호 길이(8~64자), 이름이 올바르지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 가입된 이메일")
    })
    ResponseEntity<ApiResponse<MemberResponse>> signup(SignupRequest request);

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 Access Token을 발급합니다. "
                    + "이후 요청에는 Authorization: Bearer {accessToken} 헤더를 담습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않음")
    })
    ResponseEntity<ApiResponse<TokenResponse>> login(LoginRequest request);
}
