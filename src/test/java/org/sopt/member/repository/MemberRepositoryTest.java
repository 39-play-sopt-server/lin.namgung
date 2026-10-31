package org.sopt.member.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.config.JpaAuditingConfig;
import org.sopt.member.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository repository;

    @Test
    @DisplayName("저장하면 id와 생성일이 자동으로 부여된다")
    void saveAssignsIdAndCreatedAt() {
        Member saved = repository.save(Member.create("남궁린"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("탈퇴하지 않은 회원은 id로 조회된다")
    void findActiveMember() {
        Member saved = repository.save(Member.create("남궁린"));

        Member found = repository.findByIdAndDeletedFalse(saved.getId()).orElseThrow();

        assertEquals("남궁린", found.getName());
    }

    @Test
    @DisplayName("탈퇴한 회원은 조회되지 않지만 행은 그대로 남아 있다")
    void deletedMemberIsNotFoundButRowRemains() {
        Member saved = repository.save(Member.create("남궁린"));

        saved.delete();
        repository.flush();

        assertTrue(repository.findByIdAndDeletedFalse(saved.getId()).isEmpty());
        assertTrue(repository.findById(saved.getId()).isPresent());
    }

    @Test
    @DisplayName("존재하지 않는 id는 조회되지 않는다")
    void findUnknownId() {
        assertTrue(repository.findByIdAndDeletedFalse(999L).isEmpty());
    }
}
