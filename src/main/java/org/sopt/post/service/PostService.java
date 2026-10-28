package org.sopt.post.service;

import org.sopt.global.exception.ErrorCode;
import org.sopt.global.response.PageResponse;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.exception.InvalidPageRequestException;
import org.sopt.post.exception.InvalidPostException;
import org.sopt.post.exception.InvalidSearchKeywordException;
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
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(
            PostRepository postRepository,
            MemberRepository memberRepository,
            PostLikeRepository postLikeRepository
    ) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.postLikeRepository = postLikeRepository;
    }

    @Transactional
    public PostResponse registerPost(String title, String content, Long memberId, Category category) {
        Member member = findActiveMember(memberId);
        Post post = Post.create(title, content, member, category);
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(Category category, int page, int size) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page - 1, size, NEWEST_FIRST);
        Page<Post> result = category == null
                ? postRepository.findAll(pageable)
                : postRepository.findByCategory(category, pageable);
        return toPageResponse(result, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> searchPostsByTitle(String keyword, int page, int size) {
        String trimmed = validateKeyword(keyword);
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page - 1, size, NEWEST_FIRST);
        Page<Post> result = postRepository.findByTitleContaining(trimmed, pageable);
        return toPageResponse(result, page, size);
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
        postLikeRepository.deleteAllByPostId(id);
        postRepository.delete(post);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException();
        }
    }

    private PageResponse<PostSummaryResponse> toPageResponse(Page<Post> result, int page, int size) {
        List<PostSummaryResponse> content = result.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    private String validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidSearchKeywordException();
        }
        return keyword.trim();
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
    }

    private Member findActiveMember(Long memberId) {
        if (memberId == null) {
            throw new InvalidPostException(ErrorCode.INVALID_AUTHOR);
        }
        return memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}