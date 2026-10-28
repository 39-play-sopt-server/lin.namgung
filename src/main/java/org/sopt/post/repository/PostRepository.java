package org.sopt.post.repository;

import jakarta.persistence.LockModeType;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    @Override
    @EntityGraph(attributePaths = "member")
    Page<Post> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "member")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "member")
    Page<Post> findByCategory(Category category, Pageable pageable);

    @EntityGraph(attributePaths = "member")
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);
}
