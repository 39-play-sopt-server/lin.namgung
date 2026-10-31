package org.sopt.like.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.exception.GlobalExceptionHandler;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.like.service.PostLikeService;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LikeControllerTest {
    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private MockMvc mockMvc;
    private Long postId;
    private Long memberId;
    private String likesUrl;

    @BeforeEach
    void setUp() {
        clear();
        Member member = memberRepository.save(Member.create("회원"));
        memberId = member.getId();
        postId = postRepository.save(Post.create("제목", "본문", member, Category.FREE)).getId();
        likesUrl = "/api/v1/posts/" + postId + "/likes";
        mockMvc = MockMvcBuilders.standaloneSetup(new LikeController(postLikeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private void clear() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("좋아요를 누르면 201과 최신 좋아요 수를 공통 응답 형식으로 반환한다")
    void like() throws Exception {
        mockMvc.perform(post(likesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(memberId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POST_LIKED"))
                .andExpect(jsonPath("$.message").value("좋아요를 눌렀습니다."))
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 글에 다시 누르면 409를 반환한다")
    void likeTwice() throws Exception {
        like();

        mockMvc.perform(post(likesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(memberId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ALREADY_LIKED"));
    }

    @Test
    @DisplayName("존재하지 않는 글에 좋아요를 누르면 404를 반환한다")
    void likeUnknownPost() throws Exception {
        mockMvc.perform(post("/api/v1/posts/999999/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 회원이 좋아요를 누르면 404를 반환한다")
    void likeUnknownMember() throws Exception {
        mockMvc.perform(post(likesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("탈퇴한 회원이 좋아요를 누르면 404를 반환한다")
    void likeWithWithdrawnMember() throws Exception {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.delete();
        memberRepository.save(member);

        mockMvc.perform(post(likesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("memberId를 생략하면 400을 반환한다")
    void likeWithoutMemberId() throws Exception {
        mockMvc.perform(post(likesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_ID"));
    }

    @Test
    @DisplayName("글 id가 숫자가 아니면 400을 반환한다")
    void likeWithInvalidPostId() throws Exception {
        mockMvc.perform(post("/api/v1/posts/abc/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(memberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("좋아요를 취소하면 200과 줄어든 좋아요 수를 반환한다")
    void unlike() throws Exception {
        like();

        mockMvc.perform(delete(likesUrl + "/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("POST_UNLIKED"))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 글을 취소하면 404를 반환한다")
    void unlikeWithoutLike() throws Exception {
        mockMvc.perform(delete(likesUrl + "/" + memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIKE_NOT_FOUND"));
    }

    private String body(Long id) {
        return "{\"memberId\": " + id + "}";
    }
}
