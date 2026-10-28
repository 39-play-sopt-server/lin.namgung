package org.sopt.post.repository;

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
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostSpecificationsTest {
    private static final Pageable PAGEABLE = PageRequest.of(0, 10,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

    @Autowired
    private PostRepository repository;

    @Autowired
    private MemberRepository memberRepository;

    private Member lin;
    private Member kim;

    @BeforeEach
    void setUp() {
        lin = memberRepository.save(Member.create("린"));
        kim = memberRepository.save(Member.create("김"));
        save("스프링 공지", lin, Category.NOTICE);
        save("스프링 공부", lin, Category.STUDY);
        save("자바 공부", lin, Category.STUDY);
        save("스프링 질문", kim, Category.QUESTION);
        save("자바 공지", kim, Category.NOTICE);
    }

    @Test
    @DisplayName("조건이 없으면 전체를 조회한다")
    void noCondition() {
        assertEquals(5, find(PostSpecifications.alwaysTrue()).getTotalElements());
    }

    @Test
    @DisplayName("작성자로만 필터링한다")
    void byMember() {
        Page<Post> result = find(PostSpecifications.hasMember(lin.getId()));

        assertEquals(3, result.getTotalElements());
    }

    @Test
    @DisplayName("카테고리로만 필터링한다")
    void byCategory() {
        Page<Post> result = find(PostSpecifications.hasCategory(Category.NOTICE));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("제목으로만 필터링한다")
    void byTitle() {
        Page<Post> result = find(PostSpecifications.titleContains("스프링"));

        assertEquals(3, result.getTotalElements());
    }

    @Test
    @DisplayName("작성자와 카테고리를 함께 적용하면 둘 다 만족하는 글만 조회한다")
    void byMemberAndCategory() {
        Specification<Post> spec = PostSpecifications.hasMember(lin.getId())
                .and(PostSpecifications.hasCategory(Category.STUDY));

        Page<Post> result = find(spec);

        assertEquals(2, result.getTotalElements());
        assertEquals("자바 공부", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("세 조건을 모두 적용할 수 있다")
    void byAllConditions() {
        Specification<Post> spec = PostSpecifications.hasMember(lin.getId())
                .and(PostSpecifications.hasCategory(Category.STUDY))
                .and(PostSpecifications.titleContains("스프링"));

        Page<Post> result = find(spec);

        assertEquals(1, result.getTotalElements());
        assertEquals("스프링 공부", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("조건에 맞는 글이 없으면 빈 결과를 반환한다")
    void noMatch() {
        Specification<Post> spec = PostSpecifications.hasMember(kim.getId())
                .and(PostSpecifications.hasCategory(Category.STUDY));

        assertEquals(0, find(spec).getTotalElements());
    }

    @Test
    @DisplayName("제목 검색어의 %와 _는 와일드카드가 아니라 문자 그대로 검색한다")
    void titleTreatsWildcardsAsLiterals() {
        save("할인 100% 이벤트", lin, Category.FREE);
        save("할인 이벤트", lin, Category.FREE);
        save("a_b 규칙", lin, Category.FREE);
        save("axb 규칙", lin, Category.FREE);

        assertEquals(1, find(PostSpecifications.titleContains("%")).getTotalElements());
        assertEquals(1, find(PostSpecifications.titleContains("a_b")).getTotalElements());
    }

    private Page<Post> find(Specification<Post> spec) {
        return repository.findAll(spec, PAGEABLE);
    }

    private void save(String title, Member member, Category category) {
        repository.save(Post.create(title, "본문", member, category));
    }
}
