package org.sopt.like.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.like.dto.response.LikeResponse;
import org.sopt.like.exception.AlreadyLikedException;
import org.sopt.like.exception.LikeNotFoundException;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.exception.InvalidMemberException;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.exception.PostNotFoundException;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PostLikeServiceTest {
    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member author;
    private Member liker;
    private Post post;

    @BeforeEach
    void setUp() {
        author = memberRepository.save(Member.create("작성자"));
        liker = memberRepository.save(Member.create("좋아요 누르는 회원"));
        post = postRepository.save(Post.create("제목", "본문", author, Category.FREE));
    }

    @Test
    @DisplayName("좋아요를 누르면 좋아요가 저장되고 좋아요 수가 1 늘어난다")
    void like() {
        LikeResponse response = postLikeService.like(post.getId(), liker.getId());

        assertEquals(post.getId(), response.postId());
        assertEquals(1, response.likeCount());
        assertTrue(postLikeRepository.existsByPostIdAndMemberId(post.getId(), liker.getId()));
        assertEquals(1, postRepository.findById(post.getId()).orElseThrow().getLikeCount());
    }

    @Test
    @DisplayName("다른 회원이 같은 글에 좋아요를 누르면 좋아요 수가 함께 늘어난다")
    void likeByDifferentMembers() {
        postLikeService.like(post.getId(), liker.getId());

        LikeResponse response = postLikeService.like(post.getId(), author.getId());

        assertEquals(2, response.likeCount());
    }

    @Test
    @DisplayName("같은 회원이 같은 글에 다시 좋아요를 누르면 AlreadyLikedException이 발생하고 수는 그대로다")
    void likeTwice() {
        postLikeService.like(post.getId(), liker.getId());

        assertThrows(AlreadyLikedException.class, () -> postLikeService.like(post.getId(), liker.getId()));

        assertEquals(1, postLikeRepository.countByPostId(post.getId()));
        assertEquals(1, postRepository.findById(post.getId()).orElseThrow().getLikeCount());
    }

    @Test
    @DisplayName("좋아요를 취소하면 좋아요가 삭제되고 좋아요 수가 1 줄어든다")
    void unlike() {
        postLikeService.like(post.getId(), liker.getId());

        LikeResponse response = postLikeService.unlike(post.getId(), liker.getId());

        assertEquals(0, response.likeCount());
        assertEquals(0, postLikeRepository.countByPostId(post.getId()));
    }

    @Test
    @DisplayName("취소한 뒤에는 다시 좋아요를 누를 수 있다")
    void likeAgainAfterUnlike() {
        postLikeService.like(post.getId(), liker.getId());
        postLikeService.unlike(post.getId(), liker.getId());

        LikeResponse response = postLikeService.like(post.getId(), liker.getId());

        assertEquals(1, response.likeCount());
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 글의 좋아요를 취소하면 LikeNotFoundException이 발생한다")
    void unlikeWithoutLike() {
        assertThrows(LikeNotFoundException.class, () -> postLikeService.unlike(post.getId(), liker.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 글에는 좋아요를 누르거나 취소할 수 없다")
    void unknownPost() {
        assertThrows(PostNotFoundException.class, () -> postLikeService.like(999999L, liker.getId()));
        assertThrows(PostNotFoundException.class, () -> postLikeService.unlike(999999L, liker.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 회원이나 탈퇴한 회원은 좋아요를 누르거나 취소할 수 없다")
    void unknownOrWithdrawnMember() {
        assertThrows(MemberNotFoundException.class, () -> postLikeService.like(post.getId(), 999999L));

        postLikeService.like(post.getId(), liker.getId());
        liker.delete();
        memberRepository.flush();

        assertThrows(MemberNotFoundException.class, () -> postLikeService.like(post.getId(), liker.getId()));
        assertThrows(MemberNotFoundException.class, () -> postLikeService.unlike(post.getId(), liker.getId()));
    }

    @Test
    @DisplayName("회원 id가 없으면 InvalidMemberException이 발생한다")
    void nullMemberId() {
        assertThrows(InvalidMemberException.class, () -> postLikeService.like(post.getId(), null));
        assertThrows(InvalidMemberException.class, () -> postLikeService.unlike(post.getId(), null));
    }
}
