package org.sopt.like.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.like.dto.request.LikeRequest;
import org.sopt.like.dto.response.LikeResponse;
import org.sopt.like.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @PathVariable(name = "postId") Long postId,
            @RequestBody LikeRequest request
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_LIKED,
                postLikeService.like(postId, request.memberId()));
    }

    @Override
    @DeleteMapping(path = "/{memberId}")
    public ResponseEntity<ApiResponse<LikeResponse>> unlike(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "memberId") Long memberId
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_UNLIKED,
                postLikeService.unlike(postId, memberId));
    }
}
