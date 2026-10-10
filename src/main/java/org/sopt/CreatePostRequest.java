package org.sopt;

public record CreatePostRequest(
        String title,
        String content,
        String author,
        int category
) {
}