package org.sopt.post.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostRepositoryTest {
    private final PostRepository repository = new PostRepository();

    @Test
    @DisplayName("저장하면 1부터 순서대로 id가 부여된다")
    void saveAssignsSequentialIds() {
        Post first = save("첫 번째", Category.FREE);
        Post second = save("두 번째", Category.FREE);

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
    }

    @Test
    @DisplayName("게시글을 삭제해도 이후 id는 이어서 부여되고 다른 게시글의 id는 바뀌지 않는다")
    void deleteDoesNotShiftIds() {
        Post first = save("첫 번째", Category.FREE);
        Post second = save("두 번째", Category.FREE);

        repository.deleteById(first.getId());
        Post third = save("세 번째", Category.FREE);

        assertNull(repository.findById(first.getId()));
        assertEquals(second, repository.findById(2L));
        assertEquals(3L, third.getId());
    }

    @Test
    @DisplayName("목록은 최신 글(id가 큰 글)부터 반환한다")
    void findPageOrdersByNewestFirst() {
        save("a", Category.FREE);
        save("b", Category.FREE);
        save("c", Category.FREE);

        List<Post> posts = repository.findPage(null, 0, 2);

        assertEquals(2, posts.size());
        assertEquals("c", posts.get(0).getTitle());
        assertEquals("b", posts.get(1).getTitle());
    }

    @Test
    @DisplayName("offset만큼 건너뛰고 limit만큼 반환한다")
    void findPageWithOffset() {
        save("a", Category.FREE);
        save("b", Category.FREE);
        save("c", Category.FREE);

        List<Post> posts = repository.findPage(null, 2, 2);

        assertEquals(1, posts.size());
        assertEquals("a", posts.get(0).getTitle());
    }

    @Test
    @DisplayName("카테고리를 지정하면 해당 카테고리의 게시글만 조회하고 개수도 그 기준으로 센다")
    void filterByCategory() {
        save("공지1", Category.NOTICE);
        save("자유1", Category.FREE);
        save("공지2", Category.NOTICE);

        List<Post> notices = repository.findPage(Category.NOTICE, 0, 10);

        assertEquals(2, notices.size());
        assertEquals("공지2", notices.get(0).getTitle());
        assertEquals("공지1", notices.get(1).getTitle());
        assertEquals(2, repository.count(Category.NOTICE));
        assertEquals(1, repository.count(Category.FREE));
        assertEquals(0, repository.count(Category.STUDY));
        assertEquals(3, repository.count(null));
    }

    private Post save(String title, Category category) {
        return repository.save(Post.create(title, "본문", "작성자", category));
    }
}
