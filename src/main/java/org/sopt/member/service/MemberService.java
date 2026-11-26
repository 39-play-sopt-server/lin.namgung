package org.sopt.member.service;

import org.sopt.member.domain.Member;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findActiveMember(memberId));
    }

    @Transactional
    public void withdrawMember(Long memberId) {
        findActiveMember(memberId).delete();
    }

    private Member findActiveMember(Long memberId) {
        return memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
