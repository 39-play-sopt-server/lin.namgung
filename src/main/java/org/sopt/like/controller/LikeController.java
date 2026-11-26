package org.sopt.like.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.like.dto.response.LikeResponse;
import org.sopt.like.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/posts/{postId}/likes")
public class LikeController implements LikeApi {
    private final PostLikeService postLikeService;

    public LikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> like(
            @AuthenticationPrincipal Long memberId,
            @PathVariable(name = "postId") Long postId
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_LIKED,
                postLikeService.like(postId, memberId));
    }

    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<LikeResponse>> unlike(
            @AuthenticationPrincipal Long memberId,
            @PathVariable(name = "postId") Long postId
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_UNLIKED,
                postLikeService.unlike(postId, memberId));
    }
}
