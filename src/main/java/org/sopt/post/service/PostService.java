package org.sopt.post.service;

import org.sopt.global.response.PageResponse;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.exception.InvalidPageRequestException;
import org.sopt.post.exception.PostNotFoundException;
import org.sopt.post.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PostService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostResponse registerPost(String title, String content, String author, Category category) {
        Post post = Post.create(title, content, author, category);
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(Category category, int page, int size) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Post> result = category == null
                ? postRepository.findAll(pageable)
                : postRepository.findByCategory(category, pageable);
        List<PostSummaryResponse> content = result.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = findPost(id);
        post.increaseViewCount();
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse changePost(Long id, String title, String content) {
        Post post = findPost(id);
        post.update(title, content);
        return PostResponse.from(post);
    }

    @Transactional
    public void removePost(Long id) {
        Post post = findPost(id);
        postRepository.delete(post);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException();
        }
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
    }
}