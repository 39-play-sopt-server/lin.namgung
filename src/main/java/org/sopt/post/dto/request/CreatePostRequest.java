package org.sopt.post.dto.request;

public record CreatePostRequest(
        String title,
        String content,
        String author,
        int category
) {
}