package org.sopt.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.member.domain.Member;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MemberServiceTest {
    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 정보를 조회하면 id, 이름, 이메일을 반환한다")
    void getMember() {
        Member member = memberRepository.save(Member.createWithEmail("린", "lin@sopt.org", "encoded"));

        MemberResponse response = memberService.getMember(member.getId());

        assertEquals(member.getId(), response.id());
        assertEquals("린", response.name());
        assertEquals("lin@sopt.org", response.email());
    }

    @Test
    @DisplayName("탈퇴한 회원의 정보는 조회할 수 없다")
    void getWithdrawnMember() {
        Member member = memberRepository.save(Member.create("린"));
        memberService.withdrawMember(member.getId());
        memberRepository.flush();

        assertThrows(MemberNotFoundException.class, () -> memberService.getMember(member.getId()));
    }

    @Test
    @DisplayName("탈퇴하면 회원은 삭제되지 않고 탈퇴 상태로 남는다")
    void withdrawKeepsRow() {
        Member member = memberRepository.save(Member.create("린"));

        memberService.withdrawMember(member.getId());
        memberRepository.flush();

        assertTrue(memberRepository.findById(member.getId()).orElseThrow().isDeleted());
        assertTrue(memberRepository.findByIdAndDeletedFalse(member.getId()).isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 탈퇴할 수 없다")
    void withdrawUnknownMember() {
        assertThrows(MemberNotFoundException.class, () -> memberService.withdrawMember(999999L));
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void withdrawTwice() {
        Member member = memberRepository.save(Member.create("린"));
        memberService.withdrawMember(member.getId());
        memberRepository.flush();

        assertThrows(MemberNotFoundException.class, () -> memberService.withdrawMember(member.getId()));
    }
}
