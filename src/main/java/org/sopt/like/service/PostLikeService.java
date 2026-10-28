package org.sopt.like.service;

import org.sopt.global.exception.ErrorCode;
import org.sopt.like.domain.PostLike;
import org.sopt.like.dto.response.LikeResponse;
import org.sopt.like.exception.AlreadyLikedException;
import org.sopt.like.exception.LikeNotFoundException;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.exception.InvalidMemberException;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Post;
import org.sopt.post.exception.PostNotFoundException;
import org.sopt.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostLikeService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostLikeRepository postLikeRepository;

    public PostLikeService(
            PostRepository postRepository,
            MemberRepository memberRepository,
            PostLikeRepository postLikeRepository
    ) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.postLikeRepository = postLikeRepository;
    }

    @Transactional
    public LikeResponse like(Long postId, Long memberId) {
        Member member = findActiveMember(memberId);
        Post post = lockPost(postId);
        if (postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new AlreadyLikedException();
        }
        postLikeRepository.save(PostLike.create(post, member));
        post.increaseLikeCount();
        return LikeResponse.from(post);
    }

    @Transactional
    public LikeResponse unlike(Long postId, Long memberId) {
        findActiveMember(memberId);
        Post post = lockPost(postId);
        PostLike like = postLikeRepository.findByPostIdAndMemberId(postId, memberId)
                .orElseThrow(LikeNotFoundException::new);
        postLikeRepository.delete(like);
        post.decreaseLikeCount();
        return LikeResponse.from(post);
    }

    private Member findActiveMember(Long memberId) {
        if (memberId == null) {
            throw new InvalidMemberException(ErrorCode.INVALID_MEMBER_ID);
        }
        return memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private Post lockPost(Long postId) {
        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(PostNotFoundException::new);
    }
}
