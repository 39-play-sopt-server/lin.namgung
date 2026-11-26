package org.sopt.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.global.response.ApiResponse;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "회원", description = "내 정보 조회, 회원 탈퇴 API (로그인 필요)")
public interface MemberApi {

    @Operation(summary = "내 정보 조회", description = "Access Token의 회원 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음, 또는 만료됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<MemberResponse>> getMe(@Parameter(hidden = true) Long memberId);

    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 회원을 탈퇴 처리합니다. 회원이 쓴 게시글은 삭제되지 않고, 작성자가 '탈퇴한 회원'으로 표시됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음, 또는 만료됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "이미 탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<Void>> deleteMe(@Parameter(hidden = true) Long memberId);
}
