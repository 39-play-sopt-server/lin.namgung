package org.sopt.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.response.PageResponse;
import org.sopt.member.domain.Member;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.exception.InvalidPageRequestException;
import org.sopt.post.exception.InvalidPostException;
import org.sopt.post.exception.PostNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PostServiceTest {
    @Autowired
    private PostService postService;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("작성자"));
    }

    @Test
    @DisplayName("게시글을 작성하면 id가 부여된 게시글 정보를 반환한다")
    void register() {
        PostResponse response = postService.registerPost("제목", "본문", member.getId(),Category.STUDY);

        assertNotNull(response.id());
        assertEquals("제목", response.title());
        assertEquals(Category.STUDY, response.category());
        assertEquals(0, response.viewCount());
    }

    @Test
    @DisplayName("게시글을 작성하면 응답의 작성자는 회원의 이름이다")
    void registerShowsMemberName() {
        PostResponse response = postService.registerPost("제목", "본문", member.getId(), Category.FREE);

        assertEquals("작성자", response.author());
    }

    @Test
    @DisplayName("존재하지 않는 회원으로는 게시글을 작성할 수 없다")
    void registerWithUnknownMember() {
        assertThrows(MemberNotFoundException.class,
                () -> postService.registerPost("제목", "본문", 999999L, Category.FREE));
    }

    @Test
    @DisplayName("회원 id가 없으면 InvalidPostException이 발생한다")
    void registerWithoutMemberId() {
        assertThrows(InvalidPostException.class,
                () -> postService.registerPost("제목", "본문", null, Category.FREE));
    }

    @Test
    @DisplayName("게시글을 상세 조회할 때마다 조회수가 1씩 증가한다")
    void getPostIncreasesViewCount() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(),Category.FREE);

        assertEquals(1, postService.getPost(created.id()).viewCount());
        assertEquals(2, postService.getPost(created.id()).viewCount());
        assertEquals(3, postService.getPost(created.id()).viewCount());
    }

    @Test
    @DisplayName("목록 조회는 조회수를 증가시키지 않는다")
    void getPostsDoesNotIncreaseViewCount() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(),Category.FREE);

        postService.getPosts(null, 1, 10);
        postService.getPosts(null, 1, 10);

        assertEquals(1, postService.getPost(created.id()).viewCount());
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 PostNotFoundException이 발생한다")
    void getPostNotFound() {
        assertThrows(PostNotFoundException.class, () -> postService.getPost(999L));
    }

    @Test
    @DisplayName("수정 시 게시글 존재 여부를 값 검증보다 먼저 확인한다")
    void changePostChecksExistenceFirst() {
        assertThrows(PostNotFoundException.class, () -> postService.changePost(999L, " ", null));
    }

    @Test
    @DisplayName("수정은 전달한 필드만 바꾼다")
    void changePostPartially() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(),Category.FREE);

        PostResponse changed = postService.changePost(created.id(), "새 제목", null);

        assertEquals("새 제목", changed.title());
        assertEquals("본문", changed.content());
    }

    @Test
    @DisplayName("수정할 값이 없으면 InvalidPostException이 발생한다")
    void changePostWithNothing() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(),Category.FREE);

        assertThrows(InvalidPostException.class, () -> postService.changePost(created.id(), null, null));
    }

    @Test
    @DisplayName("삭제한 게시글은 더 이상 조회할 수 없다")
    void removePost() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(),Category.FREE);

        postService.removePost(created.id());

        assertThrows(PostNotFoundException.class, () -> postService.getPost(created.id()));
        assertThrows(PostNotFoundException.class, () -> postService.removePost(created.id()));
    }

    @Test
    @DisplayName("페이지네이션은 최신 글부터 page, size에 맞게 잘라서 반환한다")
    void getPostsWithPagination() {
        for (int i = 1; i <= 25; i++) {
            postService.registerPost("제목" + i, "본문", member.getId(),Category.FREE);
        }

        PageResponse<PostSummaryResponse> firstPage = postService.getPosts(null, 1, 10);
        PageResponse<PostSummaryResponse> lastPage = postService.getPosts(null, 3, 10);
        PageResponse<PostSummaryResponse> emptyPage = postService.getPosts(null, 4, 10);

        assertEquals(10, firstPage.content().size());
        assertEquals("제목25", firstPage.content().get(0).title());
        assertEquals(25, firstPage.totalElements());
        assertEquals(3, firstPage.totalPages());
        assertEquals(5, lastPage.content().size());
        assertEquals("제목1", lastPage.content().get(4).title());
        assertEquals(0, emptyPage.content().size());
    }

    @Test
    @DisplayName("카테고리와 페이지네이션을 함께 사용하면 전체 개수도 카테고리 기준이다")
    void getPostsWithCategory() {
        for (int i = 1; i <= 5; i++) {
            postService.registerPost("공지" + i, "본문", member.getId(),Category.NOTICE);
            postService.registerPost("자유" + i, "본문", member.getId(),Category.FREE);
        }

        PageResponse<PostSummaryResponse> page = postService.getPosts(Category.NOTICE, 1, 3);

        assertEquals(3, page.content().size());
        assertEquals("공지5", page.content().get(0).title());
        assertEquals(5, page.totalElements());
        assertEquals(2, page.totalPages());
    }

    @Test
    @DisplayName("page는 1 이상, size는 1 이상 100 이하여야 한다")
    void getPostsWithInvalidPageRequest() {
        assertThrows(InvalidPageRequestException.class, () -> postService.getPosts(null, 0, 10));
        assertThrows(InvalidPageRequestException.class, () -> postService.getPosts(null, -1, 10));
        assertThrows(InvalidPageRequestException.class, () -> postService.getPosts(null, 1, 0));
        assertThrows(InvalidPageRequestException.class, () -> postService.getPosts(null, 1, 101));
    }
}
