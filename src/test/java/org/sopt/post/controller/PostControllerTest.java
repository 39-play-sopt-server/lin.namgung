package org.sopt.post.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.exception.GlobalExceptionHandler;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.repository.PostRepository;
import org.sopt.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PostControllerTest {
    private static final String BASE_URL = "/api/v1/posts";

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    private Long memberId;

    @BeforeEach
    void setUp() {
        resetPosts();
        memberId = memberRepository.save(Member.create("작성자")).getId();
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private void resetPosts() {
        postRepository.deleteAll();
        memberRepository.deleteAll();
        jdbcTemplate.execute("ALTER TABLE post ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    @DisplayName("게시글을 작성하면 201과 Location 헤더, 생성된 게시글을 공통 응답 형식으로 반환한다")
    void createPost() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("제목", "본문", memberId,"NOTICE")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POST_CREATED"))
                .andExpect(jsonPath("$.message").value("게시글이 작성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.author").value("작성자"))
                .andExpect(jsonPath("$.data.category").value("NOTICE"))
                .andExpect(jsonPath("$.data.viewCount").value(0))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("제목이 비어있으면 400과 실패 응답 형식을 반환한다")
    void createPostWithBlankTitle() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(" ", "본문", memberId,"FREE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_TITLE"))
                .andExpect(jsonPath("$.message").value("제목이 비어있습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404를 반환한다")
    void createPostWithUnknownMember() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("제목", "본문", 999999L, "FREE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("memberId를 생략하면 400을 반환한다")
    void createPostWithoutMemberId() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"제목\", \"content\": \"본문\", \"category\": \"FREE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHOR"));
    }

    @Test
    @DisplayName("카테고리를 생략하면 400을 반환한다")
    void createPostWithoutCategory() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"제목\", \"content\": \"본문\", \"memberId\": " + memberId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 값이면 400을 반환한다")
    void createPostWithUnknownCategory() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("제목", "본문", memberId,"UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    @DisplayName("JSON 형식이 잘못되었거나 본문이 없으면 400을 반환한다")
    void createPostWithMalformedBody() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 415를 반환한다")
    void createPostWithUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드이면 405를 반환한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(delete(BASE_URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("게시글을 상세 조회할 때마다 조회수가 증가한다")
    void readPostIncreasesViewCount() throws Exception {
        createPost("제목", "FREE");

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POST_READ"))
                .andExpect(jsonPath("$.data.author").value("작성자"))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    @DisplayName("제목 검색은 검색어가 포함된 글만 최신순으로 반환한다")
    void searchPostsByTitle() throws Exception {
        createPost("스프링 입문", "STUDY");
        createPost("자바 기초", "STUDY");
        createPost("스프링 심화", "STUDY");

        mockMvc.perform(get(BASE_URL + "/search").param("title", "스프링"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POSTS_READ"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 심화"))
                .andExpect(jsonPath("$.data.content[1].title").value("스프링 입문"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("match=PREFIX이면 제목이 검색어로 시작하는 글만 반환한다")
    void searchPostsByPrefix() throws Exception {
        createPost("스프링 입문", "STUDY");
        createPost("자바와 스프링", "STUDY");

        mockMvc.perform(get(BASE_URL + "/search").param("title", "스프링").param("match", "PREFIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 입문"));
    }

    @Test
    @DisplayName("match 값이 올바르지 않으면 400을 반환한다")
    void searchPostsWithInvalidMatch() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("title", "스프링").param("match", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검색 결과가 없으면 200과 빈 목록을 반환한다")
    void searchPostsWithNoResult() throws Exception {
        createPost("스프링 입문", "STUDY");

        mockMvc.perform(get(BASE_URL + "/search").param("title", "파이썬"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("검색어를 생략하거나 공백이면 400을 반환한다")
    void searchPostsWithoutKeyword() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_KEYWORD"));

        mockMvc.perform(get(BASE_URL + "/search").param("title", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_KEYWORD"));
    }

    @Test
    @DisplayName("제목 검색에서 size가 범위를 벗어나면 400을 반환한다")
    void searchPostsWithInvalidSize() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("title", "스프링").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"));
    }

    @Test
    @DisplayName("필터 검색은 작성자와 카테고리를 함께 적용해 최신순으로 반환한다")
    void filterPostsByMemberAndCategory() throws Exception {
        Long otherId = memberRepository.save(Member.create("다른 작성자")).getId();
        createPost("공지", "NOTICE");
        createPost("스터디 1", "STUDY");
        createPost("스터디 2", "STUDY");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("남의 스터디", "본문", otherId, "STUDY")));

        mockMvc.perform(get(BASE_URL + "/filter")
                        .param("memberId", String.valueOf(memberId))
                        .param("category", "STUDY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POSTS_READ"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("스터디 2"))
                .andExpect(jsonPath("$.data.content[1].title").value("스터디 1"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("필터 검색은 제목을 선택 조건으로 함께 쓸 수 있다")
    void filterPostsWithTitle() throws Exception {
        createPost("스프링 공부", "STUDY");
        createPost("자바 공부", "STUDY");

        mockMvc.perform(get(BASE_URL + "/filter")
                        .param("category", "STUDY")
                        .param("title", "스프링"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 공부"));
    }

    @Test
    @DisplayName("필터 조건을 모두 생략하면 전체 게시글을 반환한다")
    void filterPostsWithoutConditions() throws Exception {
        createPost("첫 번째", "FREE");
        createPost("두 번째", "NOTICE");

        mockMvc.perform(get(BASE_URL + "/filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("조건에 맞는 글이 없으면 200과 빈 목록을 반환한다")
    void filterPostsWithNoResult() throws Exception {
        createPost("공지", "NOTICE");

        mockMvc.perform(get(BASE_URL + "/filter")
                        .param("memberId", "999999")
                        .param("category", "NOTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("필터 검색에서 memberId나 category 형식이 잘못되면 400을 반환한다")
    void filterPostsWithInvalidParameter() throws Exception {
        mockMvc.perform(get(BASE_URL + "/filter").param("memberId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get(BASE_URL + "/filter").param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("작성자가 탈퇴해도 게시글은 남고 상세 조회에서 작성자는 '탈퇴한 회원'으로 보인다")
    void readPostOfWithdrawnMember() throws Exception {
        createPost("제목", "FREE");
        withdrawMember();

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.author").value("탈퇴한 회원"));
    }

    @Test
    @DisplayName("작성자가 탈퇴해도 목록에서 글은 유지되고 작성자는 '탈퇴한 회원'으로 보인다")
    void readPostsOfWithdrawnMember() throws Exception {
        createPost("제목", "FREE");
        withdrawMember();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].author").value("탈퇴한 회원"));
    }

    @Test
    @DisplayName("탈퇴한 회원으로는 게시글을 작성할 수 없다")
    void createPostWithWithdrawnMember() throws Exception {
        withdrawMember();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("제목", "본문", memberId, "FREE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 404를 반환한다")
    void readPostNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 id가 숫자가 아니면 400을 반환한다")
    void readPostWithInvalidId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("목록 조회는 최신 글부터 page, size에 맞게 반환하고 조회수를 증가시키지 않는다")
    void readPostsWithPagination() throws Exception {
        createPost("첫 번째", "FREE");
        createPost("두 번째", "FREE");
        createPost("세 번째", "FREE");

        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POSTS_READ"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("세 번째"))
                .andExpect(jsonPath("$.data.content[0].author").value("작성자"))
                .andExpect(jsonPath("$.data.content[0].viewCount").value(0))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get(BASE_URL).param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("첫 번째"));
    }

    @Test
    @DisplayName("page, size를 생략하면 기본값(1, 10)으로 조회한다")
    void readPostsWithDefaultPaging() throws Exception {
        createPost("제목", "FREE");

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("category를 지정하면 해당 카테고리의 게시글만 조회한다")
    void readPostsByCategory() throws Exception {
        createPost("공지1", "NOTICE");
        createPost("자유1", "FREE");
        createPost("공지2", "NOTICE");

        mockMvc.perform(get(BASE_URL).param("category", "NOTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("공지2"))
                .andExpect(jsonPath("$.data.content[1].title").value("공지1"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("page, size, category 값이 올바르지 않으면 400을 반환한다")
    void readPostsWithInvalidParameters() throws Exception {
        mockMvc.perform(get(BASE_URL).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"));

        mockMvc.perform(get(BASE_URL).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"));

        mockMvc.perform(get(BASE_URL).param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get(BASE_URL).param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("제목만 보내서 수정하면 본문은 유지되고 수정된 게시글을 반환한다")
    void updatePostPartially() throws Exception {
        createPost("제목", "FREE");

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"새 제목\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POST_UPDATED"))
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("본문"));
    }

    @Test
    @DisplayName("수정할 값이 없거나 빈 문자열이면 400을 반환한다")
    void updatePostWithInvalidValues() throws Exception {
        createPost("제목", "FREE");

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_UPDATE_REQUEST"));

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TITLE"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 수정하면 값 검증보다 먼저 404를 반환한다")
    void updatePostNotFound() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("게시글을 삭제하면 이후 조회할 수 없다")
    void deletePost() throws Exception {
        createPost("제목", "FREE");

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POST_DELETED"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    private void createPost(String title, String category) throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(title, "본문", memberId,category)))
                .andExpect(status().isCreated());
    }

    private void withdrawMember() {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.delete();
        memberRepository.save(member);
    }

    private String createBody(String title, String content, Long writerId, String category) {
        return "{\"title\": \"" + title + "\", \"content\": \"" + content
                + "\", \"memberId\": " + writerId + ", \"category\": \"" + category + "\"}";
    }
}
