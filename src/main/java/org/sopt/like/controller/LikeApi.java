package org.sopt.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.global.response.ApiResponse;
import org.sopt.like.dto.response.LikeResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "좋아요", description = "게시글 좋아요 추가, 취소 API (로그인 필요)")
public interface LikeApi {

    @Operation(
            summary = "좋아요 누르기",
            description = "로그인한 회원이 게시글에 좋아요를 누릅니다. 같은 회원은 한 글에 한 번만 누를 수 있고, 탈퇴한 회원은 누를 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "좋아요 성공 (최신 좋아요 수 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인하지 않았거나 토큰이 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 게시글이거나 탈퇴한 회원"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 좋아요를 누른 게시글")
    })
    ResponseEntity<ApiResponse<LikeResponse>> like(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "게시글 id", example = "1") Long postId
    );

    @Operation(summary = "좋아요 취소", description = "로그인한 회원이 누른 좋아요를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "좋아요 취소 성공 (최신 좋아요 수 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인하지 않았거나 토큰이 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 게시글, 탈퇴한 회원, 또는 좋아요를 누르지 않은 게시글")
    })
    ResponseEntity<ApiResponse<LikeResponse>> unlike(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "게시글 id", example = "1") Long postId
    );
}
