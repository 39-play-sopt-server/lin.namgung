package org.sopt.post.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.post.dto.request.CreatePostRequest;
import org.sopt.post.dto.request.UpdatePostRequest;
import org.sopt.post.service.PostService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/posts")
public class PostController {
	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@PostMapping
	public ApiResponse<?> createPost(
			@RequestBody(required = true) CreatePostRequest request
	) {
		postService.registerPost(request.title(), request.content(), request.author(), request.category());
		return new ApiResponse<>("게시글이 작성되었습니다.", null);
	}

	@GetMapping
	public ApiResponse<?> readPosts(
			@RequestParam(name = "page", defaultValue = "1") int page
	) {
		return new ApiResponse<>("게시글 목록 조회 성공", postService.getAllPosts());
	}

	@GetMapping(path = "/{postId}")
	public ApiResponse<?> readPost(
			@PathVariable(name = "postId") Long postId
	) {
		return new ApiResponse<>("게시글 조회 성공", postService.getPost(postId));
	}

	@PatchMapping(path = "/{postId}")
	public ApiResponse<?> updatePost(
			@PathVariable(name = "postId") Long postId,
			@RequestBody UpdatePostRequest request
	) {
		postService.changePost(postId, request.title(), request.content());
		return new ApiResponse<>("게시글이 수정되었습니다.", null);
	}

	@DeleteMapping(path = "/{postId}")
	public ApiResponse<?> deletePost(
			@PathVariable(name = "postId") Long postId
	) {
		postService.removePost(postId);
		return new ApiResponse<>("게시글이 삭제되었습니다.", null);
	}
}