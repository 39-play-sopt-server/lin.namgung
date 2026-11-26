package org.sopt.auth.service;

import org.sopt.auth.domain.RefreshToken;
import org.sopt.auth.exception.AuthException;
import org.sopt.auth.repository.RefreshTokenRepository;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    public AuthService(
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer
    ) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional
    public MemberResponse signup(String email, String password, String name) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.createWithEmail(name, normalizedEmail, passwordEncoder.encode(password));
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        if (email == null || password == null) {
            throw new AuthException(ErrorCode.LOGIN_FAILED);
        }
        Member member = memberRepository.findByEmailAndDeletedFalse(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AuthException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(ErrorCode.LOGIN_FAILED);
        }
        return tokenIssuer.issue(member.getId());
    }

    /**
     * Refresh Token Rotation: 사용한 Refresh Token은 지우고 새 토큰 쌍을 발급한다.
     * 같은 토큰으로 동시에 두 번 요청해도 삭제에 성공한 한 요청만 재발급받는다.
     */
    @Transactional
    public AuthTokens reissue(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String tokenHash = RefreshToken.hash(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (refreshToken.isExpired(LocalDateTime.now())) {
            throw new AuthException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        if (refreshTokenRepository.deleteByTokenHash(tokenHash) != 1) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = refreshToken.getMemberId();
        memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_REFRESH_TOKEN));
        return tokenIssuer.issue(memberId);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByTokenHash(RefreshToken.hash(rawRefreshToken));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new AuthException(ErrorCode.INVALID_EMAIL);
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new AuthException(ErrorCode.INVALID_EMAIL);
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
