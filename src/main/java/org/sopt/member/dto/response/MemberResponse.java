package org.sopt.member.dto.response;

import org.sopt.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getCreatedAt());
    }
}
