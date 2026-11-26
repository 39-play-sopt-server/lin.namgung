package org.sopt.post.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.auth.repository.RefreshTokenRepository;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 Security 필터 체인을 거쳐 게시글, 좋아요 API의 인증(401)과 인가(403)를 확인한다.
 */
@SpringBootTest
class PostAuthorizationTest {
    private static final String BASE_URL = "/api/v1/posts";
    private static final String POST_BODY = "{\"title\": \"제목\", \"content\": \"본문\", \"category\": \"FREE\"}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private MockMvc mockMvc;
    private String writerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        clear();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        writerToken = bearer(memberRepository.save(Member.create("작성자")).getId());
        otherToken = bearer(memberRepository.save(Member.create("다른 회원")).getId());
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private void clear() {
        refreshTokenRepository.deleteAll();
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("게시글 목록, 상세 조회는 로그인하지 않아도 할 수 있다")
    void readWithoutToken() throws Exception {
        Integer postId = createPost(writerToken);

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/" + postId)).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/search").param("title", "제목")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰 없이 게시글을 작성, 수정, 삭제하거나 좋아요를 누르면 401을 반환한다")
    void writeWithoutToken() throws Exception {
        Integer postId = createPost(writerToken);

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(patch(BASE_URL + "/" + postId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"새 제목\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE_URL + "/" + postId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_URL + "/" + postId + "/likes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 회원의 토큰으로 수정, 삭제하면 403을 반환하고 작성자 토큰으로는 할 수 있다")
    void onlyWriterCanChange() throws Exception {
        Integer postId = createPost(writerToken);

        mockMvc.perform(patch(BASE_URL + "/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"남이 바꾼 제목\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_POST_OWNER"));
        mockMvc.perform(delete(BASE_URL + "/" + postId).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + postId).header(HttpHeaders.AUTHORIZATION, writerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("좋아요는 토큰의 회원으로 누르고 취소한다")
    void likeWithToken() throws Exception {
        Integer postId = createPost(writerToken);

        mockMvc.perform(post(BASE_URL + "/" + postId + "/likes").header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mockMvc.perform(delete(BASE_URL + "/" + postId + "/likes").header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    private Integer createPost(String token) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private String bearer(Long memberId) {
        return "Bearer " + jwtProvider.createAccessToken(memberId);
    }
}
