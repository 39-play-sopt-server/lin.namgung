package org.sopt.post.repository;

import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.springframework.data.jpa.domain.Specification;

public final class PostSpecifications {
    private static final char ESCAPE = '\\';

    private PostSpecifications() {
    }

    public static Specification<Post> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Post> hasMember(Long memberId) {
        return (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId);
    }

    public static Specification<Post> hasCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Post> titleContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("title"), "%" + escapeLike(keyword) + "%", ESCAPE);
    }

    private static String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
