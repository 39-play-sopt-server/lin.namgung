package org.sopt.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.response.PageResponse;
import org.sopt.like.domain.PostLike;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.SearchMatch;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.dto.response.PostSummaryResponse;
import org.sopt.post.exception.InvalidPageRequestException;
import org.sopt.post.exception.InvalidPostException;
import org.sopt.post.exception.InvalidSearchKeywordException;
import org.sopt.post.exception.PostNotFoundException;
import org.sopt.post.repository.PostRepository;
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

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

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
    @DisplayName("탈퇴한 회원으로는 게시글을 작성할 수 없다")
    void registerWithWithdrawnMember() {
        member.delete();
        memberRepository.flush();

        assertThrows(MemberNotFoundException.class,
                () -> postService.registerPost("제목", "본문", member.getId(), Category.FREE));
    }

    @Test
    @DisplayName("작성자가 탈퇴하면 기존 게시글은 유지되고 작성자는 '탈퇴한 회원'으로 보인다")
    void postsRemainAfterMemberWithdraws() {
        PostResponse created = postService.registerPost("제목", "본문", member.getId(), Category.FREE);
        member.delete();
        memberRepository.flush();

        assertEquals("탈퇴한 회원", postService.getPost(created.id()).author());
        assertEquals("탈퇴한 회원", postService.getPosts(null, 1, 10).content().get(0).author());
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
    @DisplayName("좋아요가 달린 게시글도 삭제할 수 있고, 그 글의 좋아요만 함께 삭제된다")
    void removePostWithLikes() {
        PostResponse target = postService.registerPost("지울 글", "본문", member.getId(), Category.FREE);
        PostResponse other = postService.registerPost("남길 글", "본문", member.getId(), Category.FREE);
        Member liker = memberRepository.save(Member.create("좋아요 누른 회원"));
        postLikeRepository.save(PostLike.create(postRepository.findById(target.id()).orElseThrow(), liker));
        postLikeRepository.save(PostLike.create(postRepository.findById(other.id()).orElseThrow(), liker));

        postService.removePost(target.id());

        assertEquals(0, postLikeRepository.countByPostId(target.id()));
        assertEquals(1, postLikeRepository.countByPostId(other.id()));
        assertThrows(PostNotFoundException.class, () -> postService.getPost(target.id()));
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
    @DisplayName("제목 검색은 검색어가 포함된 글만 최신순으로 페이지에 맞게 반환한다")
    void searchPostsByTitle() {
        postService.registerPost("스프링 입문", "본문", member.getId(), Category.STUDY);
        postService.registerPost("자바 기초", "본문", member.getId(), Category.STUDY);
        postService.registerPost("스프링 심화", "본문", member.getId(), Category.STUDY);

        PageResponse<PostSummaryResponse> page = postService.searchPostsByTitle("스프링", SearchMatch.CONTAINS, 1, 1);

        assertEquals(1, page.content().size());
        assertEquals("스프링 심화", page.content().get(0).title());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
    }

    @Test
    @DisplayName("앞부분 일치 검색은 제목이 검색어로 시작하는 글만 반환한다")
    void searchPostsByPrefix() {
        postService.registerPost("스프링 입문", "본문", member.getId(), Category.STUDY);
        postService.registerPost("자바와 스프링", "본문", member.getId(), Category.STUDY);
        postService.registerPost("스프링 심화", "본문", member.getId(), Category.STUDY);

        PageResponse<PostSummaryResponse> prefix =
                postService.searchPostsByTitle("스프링", SearchMatch.PREFIX, 1, 10);
        PageResponse<PostSummaryResponse> contains =
                postService.searchPostsByTitle("스프링", SearchMatch.CONTAINS, 1, 10);

        assertEquals(2, prefix.totalElements());
        assertEquals("스프링 심화", prefix.content().get(0).title());
        assertEquals("스프링 입문", prefix.content().get(1).title());
        assertEquals(3, contains.totalElements());
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 무시한다")
    void searchPostsTrimsKeyword() {
        postService.registerPost("스프링 입문", "본문", member.getId(), Category.STUDY);

        assertEquals(1, postService.searchPostsByTitle("  스프링  ", SearchMatch.CONTAINS, 1, 10).totalElements());
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
    void searchPostsWithNoResult() {
        postService.registerPost("스프링 입문", "본문", member.getId(), Category.STUDY);

        PageResponse<PostSummaryResponse> page = postService.searchPostsByTitle("파이썬", SearchMatch.CONTAINS, 1, 10);

        assertEquals(0, page.content().size());
        assertEquals(0, page.totalElements());
    }

    @Test
    @DisplayName("검색어가 null이거나 공백이면 InvalidSearchKeywordException이 발생한다")
    void searchPostsWithBlankKeyword() {
        assertThrows(InvalidSearchKeywordException.class, () -> postService.searchPostsByTitle(null, SearchMatch.CONTAINS, 1, 10));
        assertThrows(InvalidSearchKeywordException.class, () -> postService.searchPostsByTitle("   ", SearchMatch.CONTAINS, 1, 10));
    }

    @Test
    @DisplayName("제목 검색에도 page, size 범위 검증이 적용된다")
    void searchPostsWithInvalidPageRequest() {
        assertThrows(InvalidPageRequestException.class, () -> postService.searchPostsByTitle("스프링", SearchMatch.CONTAINS, 0, 10));
        assertThrows(InvalidPageRequestException.class, () -> postService.searchPostsByTitle("스프링", SearchMatch.CONTAINS, 1, 101));
    }

    @Test
    @DisplayName("필터 검색은 들어온 조건만 적용한다")
    void filterPostsAppliesOnlyGivenConditions() {
        Member other = memberRepository.save(Member.create("다른 작성자"));
        postService.registerPost("스프링 공지", "본문", member.getId(), Category.NOTICE);
        postService.registerPost("스프링 공부", "본문", member.getId(), Category.STUDY);
        postService.registerPost("자바 공부", "본문", other.getId(), Category.STUDY);

        assertEquals(3, postService.filterPosts(null, null, null, 1, 10).totalElements());
        assertEquals(2, postService.filterPosts(member.getId(), null, null, 1, 10).totalElements());
        assertEquals(2, postService.filterPosts(null, Category.STUDY, null, 1, 10).totalElements());
        assertEquals(1, postService.filterPosts(member.getId(), Category.STUDY, null, 1, 10).totalElements());
        assertEquals(1, postService.filterPosts(member.getId(), Category.STUDY, "스프링", 1, 10).totalElements());
    }

    @Test
    @DisplayName("필터 검색의 제목은 선택이라서 공백이면 조건에서 제외한다")
    void filterPostsIgnoresBlankTitle() {
        postService.registerPost("스프링 공지", "본문", member.getId(), Category.NOTICE);

        assertEquals(1, postService.filterPosts(member.getId(), null, "   ", 1, 10).totalElements());
    }

    @Test
    @DisplayName("탈퇴한 회원의 글도 작성자 id로 필터링할 수 있고 작성자는 '탈퇴한 회원'으로 보인다")
    void filterPostsOfWithdrawnMember() {
        postService.registerPost("제목", "본문", member.getId(), Category.FREE);
        member.delete();
        memberRepository.flush();

        PageResponse<PostSummaryResponse> page = postService.filterPosts(member.getId(), null, null, 1, 10);

        assertEquals(1, page.totalElements());
        assertEquals("탈퇴한 회원", page.content().get(0).author());
    }

    @Test
    @DisplayName("필터 검색에도 page, size 범위 검증이 적용된다")
    void filterPostsWithInvalidPageRequest() {
        assertThrows(InvalidPageRequestException.class, () -> postService.filterPosts(null, null, null, 0, 10));
        assertThrows(InvalidPageRequestException.class, () -> postService.filterPosts(null, null, null, 1, 101));
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
