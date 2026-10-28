package org.sopt.like.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.config.JpaAuditingConfig;
import org.sopt.like.domain.PostLike;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostLikeRepositoryTest {
    @Autowired
    private PostLikeRepository repository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member lin;
    private Member kim;
    private Post post;
    private Post otherPost;

    @BeforeEach
    void setUp() {
        lin = memberRepository.save(Member.create("린"));
        kim = memberRepository.save(Member.create("김"));
        post = postRepository.save(Post.create("제목", "본문", lin, Category.FREE));
        otherPost = postRepository.save(Post.create("다른 글", "본문", lin, Category.FREE));
    }

    @Test
    @DisplayName("좋아요를 저장하면 id와 생성일이 기록되고, 글과 회원으로 조회할 수 있다")
    void save() {
        PostLike saved = repository.save(PostLike.create(post, kim));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertTrue(repository.existsByPostIdAndMemberId(post.getId(), kim.getId()));
        assertEquals(saved.getId(), repository.findByPostIdAndMemberId(post.getId(), kim.getId()).orElseThrow().getId());
    }

    @Test
    @DisplayName("좋아요를 누르지 않았으면 존재하지 않는 것으로 조회된다")
    void notLiked() {
        repository.save(PostLike.create(post, kim));

        assertFalse(repository.existsByPostIdAndMemberId(post.getId(), lin.getId()));
        assertFalse(repository.existsByPostIdAndMemberId(otherPost.getId(), kim.getId()));
        assertTrue(repository.findByPostIdAndMemberId(otherPost.getId(), kim.getId()).isEmpty());
    }

    @Test
    @DisplayName("같은 회원이 같은 글에 두 번 좋아요를 저장하면 유니크 제약으로 막힌다")
    void duplicateLikeIsRejectedByDatabase() {
        repository.saveAndFlush(PostLike.create(post, kim));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(PostLike.create(post, kim)));
    }

    @Test
    @DisplayName("같은 회원이 다른 글에, 다른 회원이 같은 글에는 좋아요를 누를 수 있다")
    void sameMemberOtherPostAndOtherMemberSamePost() {
        repository.saveAndFlush(PostLike.create(post, kim));

        repository.saveAndFlush(PostLike.create(otherPost, kim));
        repository.saveAndFlush(PostLike.create(post, lin));

        assertEquals(2, repository.countByPostId(post.getId()));
        assertEquals(1, repository.countByPostId(otherPost.getId()));
    }

    @Test
    @DisplayName("글 기준으로 좋아요를 한 번에 삭제하면 다른 글의 좋아요는 남는다")
    void deleteAllByPostId() {
        repository.save(PostLike.create(post, kim));
        repository.save(PostLike.create(post, lin));
        repository.save(PostLike.create(otherPost, kim));

        repository.deleteAllByPostId(post.getId());

        assertEquals(0, repository.countByPostId(post.getId()));
        assertEquals(1, repository.countByPostId(otherPost.getId()));
    }
}
