package org.sopt;

public record UpdatePostRequest(
        String title,
        String content
) {
}