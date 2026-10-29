package org.sopt.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.PageResponse;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.SearchMatch;
import org.sopt.post.dto.request.CreatePostRequest;
import org.sopt.post.dto.request.UpdatePostRequest;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "게시글", description = "게시글 작성, 조회, 수정, 삭제 API")
public interface PostApi {

    @Operation(
            summary = "게시글 작성",
            description = "제목, 본문, 작성자(memberId), 카테고리로 게시글을 작성합니다. 탈퇴한 회원은 작성할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "게시글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "제목, 본문, memberId, 카테고리가 비어있거나 요청 형식이 올바르지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않거나 탈퇴한 회원")
    })
    ResponseEntity<ApiResponse<PostResponse>> createPost(CreatePostRequest request);

    @Operation(
            summary = "게시글 목록 조회",
            description = "최신 글부터 페이지 단위로 조회합니다. category를 생략하면 전체 게시글을 조회합니다. "
                    + "작성자가 탈퇴한 글은 작성자가 '탈퇴한 회원'으로 표시됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "게시글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "category 값이 올바르지 않거나 page, size가 범위를 벗어남")
    })
    ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> readPosts(
            @Parameter(description = "조회할 카테고리 (NOTICE, QUESTION, STUDY, FREE). 생략하면 전체 조회")
            Category category,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            int page,
            @Parameter(description = "페이지 크기 (1 이상 100 이하)", example = "10")
            int size
    );

    @Operation(
            summary = "게시글 제목 검색",
            description = "제목에 검색어가 포함된 게시글을 최신 글부터 페이지 단위로 조회합니다. 검색어 앞뒤 공백은 무시합니다. "
                    + "match=PREFIX이면 제목이 검색어로 시작하는 글만 조회하며, 제목 인덱스를 사용해 데이터가 많아도 빠릅니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "게시글 검색 성공 (결과가 없으면 빈 목록)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "검색어가 비어있거나 match 값이 올바르지 않거나 page, size가 범위를 벗어남")
    })
    ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> searchPosts(
            @Parameter(description = "제목 검색어", example = "스프링")
            String title,
            @Parameter(description = "검색 방식 (CONTAINS: 포함, PREFIX: 앞부분 일치). 생략하면 CONTAINS", example = "CONTAINS")
            SearchMatch match,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            int page,
            @Parameter(description = "페이지 크기 (1 이상 100 이하)", example = "10")
            int size
    );

    @Operation(
            summary = "게시글 필터 검색",
            description = "작성자 id, 카테고리, 제목 중 입력한 조건만 적용해 최신 글부터 조회합니다. "
                    + "조건을 모두 생략하면 전체 게시글을 조회하고, 제목이 비어있으면 제목 조건은 무시합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "필터 검색 성공 (결과가 없으면 빈 목록)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "memberId, category 형식이 올바르지 않거나 page, size가 범위를 벗어남")
    })
    ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> filterPosts(
            @Parameter(description = "작성자(회원) id. 생략하면 작성자 조건 없음", example = "1")
            Long memberId,
            @Parameter(description = "카테고리 (NOTICE, QUESTION, STUDY, FREE). 생략하면 카테고리 조건 없음")
            Category category,
            @Parameter(description = "제목 검색어 (선택)", example = "스프링")
            String title,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            int page,
            @Parameter(description = "페이지 크기 (1 이상 100 이하)", example = "10")
            int size
    );

    @Operation(summary = "게시글 상세 조회", description = "게시글을 조회하며, 조회할 때마다 조회수가 1 증가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "게시글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 게시글")
    })
    ResponseEntity<ApiResponse<PostResponse>> readPost(
            @Parameter(description = "게시글 id", example = "1")
            Long postId
    );

    @Operation(
            summary = "게시글 수정",
            description = "전달한 필드만 수정합니다. 생략(null)한 필드는 기존 값을 유지하고, 빈 문자열은 허용하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "게시글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "수정할 값이 없거나 제목, 본문이 빈 문자열임"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 게시글")
    })
    ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @Parameter(description = "게시글 id", example = "1")
            Long postId,
            UpdatePostRequest request
    );

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 게시글")
    })
    ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 id", example = "1")
            Long postId
    );
}
