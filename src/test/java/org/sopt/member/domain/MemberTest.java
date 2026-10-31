package org.sopt.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.exception.InvalidMemberException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    @Test
    @DisplayName("회원을 생성하면 탈퇴하지 않은 상태이고 id와 생성일은 저장되기 전까지 부여되지 않는다")
    void create() {
        Member member = Member.create("남궁린");

        assertEquals("남궁린", member.getName());
        assertFalse(member.isDeleted());
        assertNull(member.getId());
        assertNull(member.getCreatedAt());
    }

    @Test
    @DisplayName("이름이 null이거나 공백이면 생성할 수 없다")
    void createWithInvalidName() {
        assertCreateFails(null);
        assertCreateFails("");
        assertCreateFails("   ");
    }

    @Test
    @DisplayName("탈퇴하면 삭제 표시가 되고 이름은 그대로 남는다")
    void delete() {
        Member member = Member.create("남궁린");

        member.delete();

        assertTrue(member.isDeleted());
        assertEquals("남궁린", member.getName());
    }

    @Test
    @DisplayName("탈퇴하지 않은 회원은 이름을 그대로 보여준다")
    void displayNameOfActiveMember() {
        Member member = Member.create("남궁린");

        assertEquals("남궁린", member.getDisplayName());
    }

    @Test
    @DisplayName("탈퇴한 회원은 이름 대신 '탈퇴한 회원'으로 보여준다")
    void displayNameOfDeletedMember() {
        Member member = Member.create("남궁린");

        member.delete();

        assertEquals("탈퇴한 회원", member.getDisplayName());
        assertEquals("남궁린", member.getName());
    }

    private void assertCreateFails(String name) {
        InvalidMemberException e = assertThrows(InvalidMemberException.class, () -> Member.create(name));
        assertEquals(ErrorCode.INVALID_MEMBER_NAME, e.getErrorCode());
    }
}
