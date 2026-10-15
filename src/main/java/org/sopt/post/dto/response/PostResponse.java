package org.sopt.post.dto.response;

import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        String author,
        Category category,
        LocalDateTime createdAt,
        int viewCount
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCategory(),
                post.getCreatedAt(),
                post.getViewCount()
        );
    }
}
