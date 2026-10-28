package org.sopt.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.global.response.ApiResponse;
import org.sopt.member.dto.request.CreateMemberRequest;
import org.sopt.member.dto.response.MemberResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "회원", description = "회원 등록, 탈퇴 API")
public interface MemberApi {

    @Operation(summary = "회원 등록", description = "이름으로 회원을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "회원 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이름이 비어있거나 요청 형식이 올바르지 않음")
    })
    ResponseEntity<ApiResponse<MemberResponse>> createMember(CreateMemberRequest request);

    @Operation(
            summary = "회원 탈퇴",
            description = "회원을 탈퇴 처리합니다. 회원이 쓴 게시글은 삭제되지 않고, 작성자가 '탈퇴한 회원'으로 표시됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않거나 이미 탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<Void>> deleteMember(
            @Parameter(description = "회원 id", example = "1")
            Long memberId
    );
}
