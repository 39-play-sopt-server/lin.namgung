package org.sopt.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.auth.dto.request.LoginRequest;
import org.sopt.auth.dto.request.SignupRequest;
import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.global.response.ApiResponse;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급, 로그아웃 API")
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
            description = "이메일과 비밀번호로 로그인합니다. Access Token은 응답 본문으로, "
                    + "Refresh Token은 HttpOnly 쿠키(refresh_token)로 발급합니다. "
                    + "이후 요청에는 Authorization: Bearer {accessToken} 헤더를 담습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않음")
    })
    ResponseEntity<ApiResponse<TokenResponse>> login(LoginRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "refresh_token 쿠키로 Access Token과 Refresh Token을 새로 발급합니다. "
                    + "사용한 Refresh Token은 폐기되므로 한 번만 쓸 수 있습니다(Rotation)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "재발급 성공 (새 refresh_token 쿠키 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "쿠키가 없거나, 이미 사용했거나, 만료된 Refresh Token")
    })
    ResponseEntity<ApiResponse<TokenResponse>> reissue(@Parameter(hidden = true) String refreshToken);

    @Operation(
            summary = "로그아웃",
            description = "서버에 저장된 Refresh Token을 지우고 쿠키를 만료시킵니다. "
                    + "Access Token은 만료될 때까지(최대 30분) 유효하므로 클라이언트에서도 버려야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그아웃 성공 (이미 로그아웃한 상태여도 200)")
    })
    ResponseEntity<ApiResponse<Void>> logout(@Parameter(hidden = true) String refreshToken);
}
