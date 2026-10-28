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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostRepositoryTest {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

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
