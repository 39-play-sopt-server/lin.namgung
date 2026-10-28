package org.sopt.post.dto.response;

import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String author,
        Category category,
        LocalDateTime createdAt,
        int viewCount,
        int likeCount
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getMember().getDisplayName(),
                post.getCategory(),
                post.getCreatedAt(),
                post.getViewCount(),
                post.getLikeCount()
        );
    }
}
