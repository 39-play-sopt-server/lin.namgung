package org.sopt.auth.service;

import org.sopt.auth.dto.response.TokenResponse;
import org.sopt.auth.exception.AuthException;
import org.sopt.auth.jwt.JwtProvider;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.domain.Member;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
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

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        if (email == null || password == null) {
            throw new AuthException(ErrorCode.LOGIN_FAILED);
        }
        Member member = memberRepository.findByEmailAndDeletedFalse(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AuthException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(ErrorCode.LOGIN_FAILED);
        }
        return new TokenResponse(jwtProvider.createAccessToken(member.getId()));
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
