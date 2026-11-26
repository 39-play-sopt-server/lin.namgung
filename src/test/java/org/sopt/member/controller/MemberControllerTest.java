package org.sopt.member.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 Security 필터 체인을 거쳐 회원가입 -> 로그인 -> 인증이 필요한 API 호출 흐름을 검증한다.
 */
@SpringBootTest
class MemberControllerTest {
    private static final String EMAIL = "lin@sopt.org";
    private static final String PASSWORD = "sopt1234!";

    @Autowired
    private WebApplicationContext context;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clear();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
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
    @DisplayName("회원가입하면 201과 가입한 회원 정보를 반환하고, 비밀번호는 응답에 담지 않는다")
    void signup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL, PASSWORD, "린")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MEMBER_CREATED"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("같은 이메일로 다시 가입하면 409를 반환한다")
    void signupWithDuplicateEmail() throws Exception {
        signupAndGetId();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL, PASSWORD, "린")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401을 반환한다")
    void loginWithWrongPassword() throws Exception {
        signupAndGetId();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("로그인으로 받은 토큰으로 내 정보를 조회할 수 있다")
    void getMeWithToken() throws Exception {
        Integer memberId = signupAndGetId();
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_READ"))
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    @DisplayName("토큰 없이 요청하면 Controller에 닿기 전에 401을 반환한다")
    void getMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("토큰의 문자를 바꿔서 보내면 401(INVALID_TOKEN)을 반환한다")
    void getMeWithTamperedToken() throws Exception {
        signupAndGetId();
        String accessToken = loginAndGetAccessToken();
        String tampered = accessToken.substring(0, accessToken.length() - 2)
                + (accessToken.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("만료된 토큰으로 요청하면 401(EXPIRED_TOKEN)을 반환한다")
    void getMeWithExpiredToken() throws Exception {
        Integer memberId = signupAndGetId();
        String expired = new JwtProvider(secret, -60_000L).createAccessToken(memberId.longValue());

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EXPIRED_TOKEN"));
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더는 토큰으로 보지 않는다")
    void getMeWithBasicHeader() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Basic bGluOnNvcHQ="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("탈퇴하면 회원은 탈퇴 상태로 남고, 같은 토큰으로 다시 탈퇴하면 404를 반환한다")
    void deleteMe() throws Exception {
        Integer memberId = signupAndGetId();
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(delete("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_DELETED"));
        assertTrue(memberRepository.findById(memberId.longValue()).orElseThrow().isDeleted());

        mockMvc.perform(delete("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    private Integer signupAndGetId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL, PASSWORD, "린")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private String loginAndGetAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }

    private String signupBody(String email, String password, String name) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\", \"name\": \"" + name + "\"}";
    }

    private String loginBody(String email, String password) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
    }
}
