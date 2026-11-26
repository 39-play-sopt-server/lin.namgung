package org.sopt.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.auth.dto.request.KakaoLoginRequest;
import org.sopt.auth.dto.request.LoginRequest;
import org.sopt.auth.dto.request.SignupRequest;
import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.global.response.ApiResponse;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "회원가입, 로그인(이메일, 카카오), 토큰 재발급, 로그아웃 API")
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

    @Operation(
            summary = "카카오 로그인 시작",
            description = "카카오 로그인 화면으로 302 리다이렉트합니다. 브라우저 주소창에 직접 입력해서 테스트할 수 있습니다. "
                    + "CSRF 방지용 state 값을 oauth_state 쿠키(5분)에 담아 콜백에서 비교합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302", description = "카카오 인가 페이지로 이동")
    })
    ResponseEntity<Void> redirectToKakao();

    @Operation(
            summary = "카카오 로그인 콜백",
            description = "카카오가 인가 코드(code)와 state를 붙여 호출하는 Redirect URI입니다. "
                    + "처음 로그인하면 자동으로 가입되고, 이메일 로그인과 같은 방식으로 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "state 값이 없거나 일치하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인가 코드가 없거나 카카오 인증에 실패함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<TokenResponse>> kakaoCallback(
            @Parameter(description = "카카오가 발급한 인가 코드") String code,
            @Parameter(description = "로그인 시작 때 보낸 state") String state,
            @Parameter(hidden = true) String savedState
    );

    @Operation(
            summary = "카카오 로그인 (클라이언트용)",
            description = "프론트엔드가 카카오 로그인 후 받은 인가 코드를 전달하면 토큰을 발급합니다. "
                    + "카카오에 등록한 Redirect URI가 서버 설정(KAKAO_REDIRECT_URI)과 같아야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인가 코드가 없거나 카카오 인증에 실패함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(KakaoLoginRequest request);
}
