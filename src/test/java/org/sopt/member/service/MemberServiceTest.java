package org.sopt.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.exception.InvalidMemberException;
import org.sopt.member.exception.MemberNotFoundException;
import org.sopt.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @DisplayName("회원을 등록하면 id와 이름, 생성일이 담긴 정보를 반환한다")
    void register() {
        MemberResponse response = memberService.registerMember("린");

        assertNotNull(response.id());
        assertEquals("린", response.name());
        assertNotNull(response.createdAt());
    }

    @Test
    @DisplayName("이름이 비어있으면 회원을 등록할 수 없다")
    void registerWithBlankName() {
        assertThrows(InvalidMemberException.class, () -> memberService.registerMember("  "));
    }

    @Test
    @DisplayName("탈퇴하면 회원은 삭제되지 않고 탈퇴 상태로 남는다")
    void withdrawKeepsRow() {
        MemberResponse created = memberService.registerMember("린");

        memberService.withdrawMember(created.id());
        memberRepository.flush();

        assertTrue(memberRepository.findById(created.id()).orElseThrow().isDeleted());
        assertTrue(memberRepository.findByIdAndDeletedFalse(created.id()).isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 탈퇴할 수 없다")
    void withdrawUnknownMember() {
        assertThrows(MemberNotFoundException.class, () -> memberService.withdrawMember(999999L));
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void withdrawTwice() {
        MemberResponse created = memberService.registerMember("린");
        memberService.withdrawMember(created.id());
        memberRepository.flush();

        assertThrows(MemberNotFoundException.class, () -> memberService.withdrawMember(created.id()));
    }
}
