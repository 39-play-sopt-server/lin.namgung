package org.sopt.auth.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.auth.repository.RefreshTokenRepository;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {
    private static final String COOKIE = RefreshTokenCookieFactory.COOKIE_NAME;
    private static final String EMAIL = "lin@sopt.org";
    private static final String PASSWORD = "sopt1234!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        clear();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"" + PASSWORD + "\", \"name\": \"린\"}"))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private void clear() {
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("로그인하면 Access Token은 본문으로, Refresh Token은 HttpOnly, Secure 쿠키로 준다")
    void loginSetsRefreshTokenCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists(COOKIE))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().secure(COOKIE, true))
                .andExpect(cookie().path(COOKIE, "/api/v1/auth"))
                .andExpect(cookie().maxAge(COOKIE, 1_209_600))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    @DisplayName("쿠키의 Refresh Token으로 재발급하면 새 Access Token과 새 쿠키를 받고, 새 토큰으로 API를 호출할 수 있다")
    void reissue() throws Exception {
        String oldRefreshToken = login().getCookie(COOKIE).getValue();

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie(COOKIE, oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOKEN_REISSUED"))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andReturn().getResponse();

        String newRefreshToken = response.getCookie(COOKIE).getValue();
        String newAccessToken = JsonPath.read(response.getContentAsString(), "$.data.accessToken");
        assertNotEquals(oldRefreshToken, newRefreshToken);
        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이미 사용한 Refresh Token으로 다시 재발급하면 401을 반환한다")
    void reissueWithUsedToken() throws Exception {
        String refreshToken = login().getCookie(COOKIE).getValue();
        mockMvc.perform(post("/api/v1/auth/reissue").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reissue").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("쿠키 없이 재발급을 요청하면 401을 반환한다")
    void reissueWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃하면 쿠키를 만료시키고, 그 Refresh Token으로는 재발급할 수 없다")
    void logout() throws Exception {
        String refreshToken = login().getCookie(COOKIE).getValue();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGOUT_SUCCESS"))
                .andExpect(cookie().maxAge(COOKIE, 0));

        mockMvc.perform(post("/api/v1/auth/reissue").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access Token이 만료돼도 로그아웃할 수 있도록 토큰 없이도 200을 반환한다")
    void logoutWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
    }

    private MockHttpServletResponse login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private String loginBody() {
        return "{\"email\": \"" + EMAIL + "\", \"password\": \"" + PASSWORD + "\"}";
    }
}
