package org.sopt.like.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.member.domain.Member;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostLikeTest {
    private final Member member = Member.create("작성자");
    private final Post post = Post.create("제목", "본문", member, Category.FREE);

    @Test
    @DisplayName("좋아요는 글과 회원을 참조하고, id와 생성일은 저장되기 전까지 부여되지 않는다")
    void create() {
        PostLike like = PostLike.create(post, member);

        assertSame(post, like.getPost());
        assertSame(member, like.getMember());
        assertNull(like.getId());
        assertNull(like.getCreatedAt());
    }

    @Test
    @DisplayName("글이나 회원이 null이면 좋아요를 만들 수 없다")
    void createWithNull() {
        assertThrows(NullPointerException.class, () -> PostLike.create(null, member));
        assertThrows(NullPointerException.class, () -> PostLike.create(post, null));
    }
}
