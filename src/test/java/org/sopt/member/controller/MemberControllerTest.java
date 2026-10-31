package org.sopt.member.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.exception.GlobalExceptionHandler;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.member.service.MemberService;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MemberControllerTest {
    private static final String BASE_URL = "/api/v1/members";

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clear();
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private void clear() {
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원을 등록하면 201과 Location 헤더, 생성된 회원을 공통 응답 형식으로 반환한다")
    void createMember() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"린\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_CREATED"))
                .andExpect(jsonPath("$.message").value("회원이 등록되었습니다."))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("린"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("이름이 비어있으면 400을 반환한다")
    void createMemberWithBlankName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_NAME"));
    }

    @Test
    @DisplayName("이름을 생략해도 400을 반환한다")
    void createMemberWithoutName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_NAME"));
    }

    @Test
    @DisplayName("회원을 탈퇴시키면 200을 반환하고 회원은 탈퇴 상태로 남는다")
    void deleteMember() throws Exception {
        Long memberId = memberRepository.save(Member.create("린")).getId();

        mockMvc.perform(delete(BASE_URL + "/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_DELETED"));

        assertTrue(memberRepository.findById(memberId).orElseThrow().isDeleted());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴시키면 404를 반환한다")
    void deleteUnknownMember() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 탈퇴한 회원을 다시 탈퇴시키면 404를 반환한다")
    void deleteMemberTwice() throws Exception {
        Long memberId = memberRepository.save(Member.create("린")).getId();
        mockMvc.perform(delete(BASE_URL + "/" + memberId)).andExpect(status().isOk());

        mockMvc.perform(delete(BASE_URL + "/" + memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("회원 id가 숫자가 아니면 400을 반환한다")
    void deleteMemberWithInvalidId() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
