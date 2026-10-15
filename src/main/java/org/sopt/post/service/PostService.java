package org.sopt.post.service;

import org.sopt.global.response.PageResponse;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.exception.InvalidPageRequestException;
import org.sopt.post.exception.PostNotFoundException;
import org.sopt.post.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PostService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public PostResponse registerPost(String title, String content, String author, Category category) {
        Post post = Post.create(title, content, author, category);
        return PostResponse.from(postRepository.save(post));
    }

    public PageResponse<PostSummaryResponse> getPosts(int page, int size) {
        validatePageRequest(page, size);
        long offset = (long) (page - 1) * size;
        List<PostSummaryResponse> content = postRepository.findPage(offset, size).stream()
                .map(PostSummaryResponse::from)
                .toList();
        return PageResponse.of(content, page, size, postRepository.count());
    }

    public PostResponse getPost(Long id) {
        Post post = findPost(id);
        post.increaseViewCount();
        return PostResponse.from(post);
    }

    public PostResponse changePost(Long id, String title, String content) {
        Post post = findPost(id);
        post.update(title, content);
        return PostResponse.from(post);
    }

    public void removePost(Long id) {
        findPost(id);
        postRepository.deleteById(id);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException();
        }
    }

    private Post findPost(Long id) {
        Post post = postRepository.findById(id);
        if (post == null) {
            throw new PostNotFoundException();
        }
        return post;
    }
}