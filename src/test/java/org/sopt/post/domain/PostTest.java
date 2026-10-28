package org.sopt.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.post.exception.InvalidPostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostTest {
    private final Member member = Member.create("작성자");

    @Test
    @DisplayName("게시글을 생성하면 조회수는 0이고 id와 생성일은 저장되기 전까지 부여되지 않는다")
    void create() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        assertEquals("제목", post.getTitle());
        assertEquals("본문", post.getContent());
        assertSame(member, post.getMember());
        assertEquals(Category.FREE, post.getCategory());
        assertEquals(0, post.getViewCount());
        assertNull(post.getId());
        assertNull(post.getCreatedAt());
    }

    @Test
    @DisplayName("제목이 null이거나 공백이면 생성할 수 없다")
    void createWithInvalidTitle() {
        assertCreateFails(null, "본문", member, Category.FREE, ErrorCode.INVALID_TITLE);
        assertCreateFails("   ", "본문", member, Category.FREE, ErrorCode.INVALID_TITLE);
    }

    @Test
    @DisplayName("본문이 null이거나 공백이면 생성할 수 없다")
    void createWithInvalidContent() {
        assertCreateFails("제목", null, member, Category.FREE, ErrorCode.INVALID_CONTENT);
        assertCreateFails("제목", "", member, Category.FREE, ErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("작성자(회원)가 null이면 생성할 수 없다")
    void createWithNullMember() {
        assertCreateFails("제목", "본문", null, Category.FREE, ErrorCode.INVALID_AUTHOR);
    }

    @Test
    @DisplayName("카테고리가 null이면 생성할 수 없다")
    void createWithNullCategory() {
        assertCreateFails("제목", "본문", member, null, ErrorCode.INVALID_CATEGORY);
    }

    @Test
    @DisplayName("제목만 수정하면 본문은 기존 값을 유지한다")
    void updateOnlyTitle() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        post.update("새 제목", null);

        assertEquals("새 제목", post.getTitle());
        assertEquals("본문", post.getContent());
    }

    @Test
    @DisplayName("본문만 수정하면 제목은 기존 값을 유지한다")
    void updateOnlyContent() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        post.update(null, "새 본문");

        assertEquals("제목", post.getTitle());
        assertEquals("새 본문", post.getContent());
    }

    @Test
    @DisplayName("수정 값이 모두 null이면 수정할 수 없다")
    void updateWithNothing() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        InvalidPostException e = assertThrows(InvalidPostException.class, () -> post.update(null, null));

        assertEquals(ErrorCode.EMPTY_UPDATE_REQUEST, e.getErrorCode());
    }

    @Test
    @DisplayName("빈 문자열로는 수정할 수 없고, 실패하면 어떤 필드도 바뀌지 않는다")
    void updateWithBlankKeepsEverything() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        InvalidPostException e = assertThrows(InvalidPostException.class, () -> post.update("새 제목", " "));

        assertEquals(ErrorCode.INVALID_CONTENT, e.getErrorCode());
        assertEquals("제목", post.getTitle());
        assertEquals("본문", post.getContent());
    }

    @Test
    @DisplayName("조회수를 증가시키면 1씩 늘어난다")
    void increaseViewCount() {
        Post post = Post.create("제목", "본문", member, Category.FREE);

        post.increaseViewCount();
        post.increaseViewCount();

        assertEquals(2, post.getViewCount());
    }

    private void assertCreateFails(
            String title, String content, Member writer, Category category, ErrorCode expected
    ) {
        InvalidPostException e = assertThrows(
                InvalidPostException.class, () -> Post.create(title, content, writer, category));
        assertEquals(expected, e.getErrorCode());
    }
}
