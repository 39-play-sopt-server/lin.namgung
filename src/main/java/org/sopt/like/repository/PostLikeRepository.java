package org.sopt.like.repository;

import org.sopt.like.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);

    long countByPostId(Long postId);

    @Modifying
    @Query("delete from PostLike l where l.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
