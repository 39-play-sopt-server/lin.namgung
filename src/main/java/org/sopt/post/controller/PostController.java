package org.sopt.post.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.PageResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.post.domain.Category;
import org.sopt.post.dto.request.CreatePostRequest;
import org.sopt.post.dto.request.UpdatePostRequest;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestBody CreatePostRequest request
    ) {
        PostResponse response = postService.registerPost(
                request.title(), request.content(), request.author(), request.category());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{postId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.of(SuccessCode.POST_CREATED, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> readPosts(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POSTS_READ, postService.getPosts(category, page, size));
    }

    @GetMapping(path = "/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> readPost(
            @PathVariable(name = "postId") Long postId
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_READ, postService.getPost(postId));
    }

    @PatchMapping(path = "/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable(name = "postId") Long postId,
            @RequestBody UpdatePostRequest request
    ) {
        return ApiResponse.toResponseEntity(SuccessCode.POST_UPDATED,
                postService.changePost(postId, request.title(), request.content()));
    }

    @DeleteMapping(path = "/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable(name = "postId") Long postId
    ) {
        postService.removePost(postId);
        return ApiResponse.toResponseEntity(SuccessCode.POST_DELETED, null);
    }
}
