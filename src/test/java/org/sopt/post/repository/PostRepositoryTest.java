package org.sopt.post.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.global.config.JpaAuditingConfig;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostRepositoryTest {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    @Autowired
    private PostRepository repository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("작성자"));
    }

    @Test
    @DisplayName("저장하면 id가 자동으로 부여되고 다시 조회할 수 있다")
    void saveAssignsId() {
        Post saved = save("제목", Category.FREE);

        assertNotNull(saved.getId());
        assertEquals("제목", repository.findById(saved.getId()).orElseThrow().getTitle());
    }

    @Test
    @DisplayName("게시글은 작성자(회원)를 참조해서 저장된다")
    void saveReferencesMember() {
        Post saved = save("제목", Category.FREE);

        Post found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(member.getId(), found.getMember().getId());
        assertEquals("작성자", found.getMember().getName());
    }

    @Test
    @DisplayName("저장하면 생성일이 자동으로 기록된다")
    void saveRecordsCreatedAt() {
        Post saved = save("제목", Category.FREE);

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("게시글을 삭제하면 조회되지 않고 다른 게시글은 유지된다")
    void delete() {
        Post first = save("첫 번째", Category.FREE);
        Post second = save("두 번째", Category.FREE);

        repository.delete(first);

        assertTrue(repository.findById(first.getId()).isEmpty());
        assertTrue(repository.findById(second.getId()).isPresent());
    }

    @Test
    @DisplayName("목록은 최신 글(id가 큰 글)부터 반환한다")
    void findAllOrdersByNewestFirst() {
        save("a", Category.FREE);
        save("b", Category.FREE);
        save("c", Category.FREE);

        Page<Post> page = repository.findAll(PageRequest.of(0, 2, NEWEST_FIRST));

        assertEquals(2, page.getContent().size());
        assertEquals("c", page.getContent().get(0).getTitle());
        assertEquals("b", page.getContent().get(1).getTitle());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    @DisplayName("잠금 조회는 글을 반환하고, 없는 글이면 비어 있다")
    void findByIdForUpdate() {
        Post saved = save("제목", Category.FREE);

        assertEquals(saved.getId(), repository.findByIdForUpdate(saved.getId()).orElseThrow().getId());
        assertTrue(repository.findByIdForUpdate(999999L).isEmpty());
    }

    @Test
    @DisplayName("정렬 기준은 id가 아니라 생성일이다")
    void findAllOrdersByCreatedAtNotId() {
        Post first = save("먼저 저장", Category.FREE);
        save("나중에 저장", Category.FREE);
        entityManager.flush();
        entityManager.createNativeQuery("update post set created_at = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.now().plusDays(1))
                .setParameter(2, first.getId())
                .executeUpdate();
        entityManager.clear();

        Page<Post> page = repository.findAll(PageRequest.of(0, 10, NEWEST_FIRST));

        assertEquals("먼저 저장", page.getContent().get(0).getTitle());
        assertEquals("나중에 저장", page.getContent().get(1).getTitle());
    }

    @Test
    @DisplayName("생성일이 같으면 id가 큰 글이 먼저 나온다")
    void findAllBreaksTiesById() {
        Post first = save("a", Category.FREE);
        Post second = save("b", Category.FREE);
        entityManager.flush();
        LocalDateTime same = LocalDateTime.now();
        entityManager.createNativeQuery("update post set created_at = ?1")
                .setParameter(1, same)
                .executeUpdate();
        entityManager.clear();

        Page<Post> page = repository.findAll(PageRequest.of(0, 10, NEWEST_FIRST));

        assertEquals(second.getId(), page.getContent().get(0).getId());
        assertEquals(first.getId(), page.getContent().get(1).getId());
    }

    @Test
    @DisplayName("페이지 번호만큼 건너뛰고 size만큼 반환한다")
    void findAllSecondPage() {
        save("a", Category.FREE);
        save("b", Category.FREE);
        save("c", Category.FREE);

        Page<Post> page = repository.findAll(PageRequest.of(1, 2, NEWEST_FIRST));

        assertEquals(1, page.getContent().size());
        assertEquals("a", page.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("카테고리를 지정하면 해당 카테고리의 게시글만 조회하고 개수도 그 기준으로 센다")
    void filterByCategory() {
        save("공지1", Category.NOTICE);
        save("자유1", Category.FREE);
        save("공지2", Category.NOTICE);
        Pageable pageable = PageRequest.of(0, 10, NEWEST_FIRST);

        Page<Post> notices = repository.findByCategory(Category.NOTICE, pageable);

        assertEquals(2, notices.getContent().size());
        assertEquals("공지2", notices.getContent().get(0).getTitle());
        assertEquals("공지1", notices.getContent().get(1).getTitle());
        assertEquals(2, notices.getTotalElements());
        assertEquals(1, repository.findByCategory(Category.FREE, pageable).getTotalElements());
        assertEquals(0, repository.findByCategory(Category.STUDY, pageable).getTotalElements());
        assertEquals(3, repository.findAll(pageable).getTotalElements());
    }

    @Test
    @DisplayName("제목에 검색어가 포함된 게시글만 최신순으로 조회하고 개수도 그 기준으로 센다")
    void searchByTitle() {
        save("스프링 시작하기", Category.STUDY);
        save("자바 기초", Category.STUDY);
        save("스프링 데이터 JPA", Category.STUDY);
        Pageable pageable = PageRequest.of(0, 10, NEWEST_FIRST);

        Page<Post> result = repository.findByTitleContaining("스프링", pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("스프링 데이터 JPA", result.getContent().get(0).getTitle());
        assertEquals("스프링 시작하기", result.getContent().get(1).getTitle());
        assertEquals(0, repository.findByTitleContaining("파이썬", pageable).getTotalElements());
    }

    @Test
    @DisplayName("검색어의 %와 _는 와일드카드가 아니라 문자 그대로 검색한다")
    void searchTreatsWildcardsAsLiterals() {
        save("할인 100% 이벤트", Category.FREE);
        save("할인 이벤트", Category.FREE);
        save("a_b 규칙", Category.FREE);
        save("axb 규칙", Category.FREE);
        Pageable pageable = PageRequest.of(0, 10, NEWEST_FIRST);

        assertEquals(1, repository.findByTitleContaining("%", pageable).getTotalElements());
        assertEquals(1, repository.findByTitleContaining("a_b", pageable).getTotalElements());
    }

    @Test
    @DisplayName("단건 조회는 작성자를 지연 로딩하고, 목록 조회는 작성자를 함께 가져온다")
    void listFetchesMemberTogether() {
        Post saved = save("제목", Category.FREE);
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        entityManager.flush();
        entityManager.clear();
        Post single = repository.findById(saved.getId()).orElseThrow();
        assertFalse(util.isLoaded(single, "member"));

        entityManager.clear();
        Post fromList = repository.findAll(PageRequest.of(0, 10, NEWEST_FIRST)).getContent().get(0);
        assertTrue(util.isLoaded(fromList, "member"));

        entityManager.clear();
        Post fromCategory = repository.findByCategory(Category.FREE, PageRequest.of(0, 10, NEWEST_FIRST))
                .getContent().get(0);
        assertTrue(util.isLoaded(fromCategory, "member"));
    }

    private Post save(String title, Category category) {
        return repository.save(Post.create(title, "본문", member, category));
    }
}
