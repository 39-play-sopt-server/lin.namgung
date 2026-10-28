package org.sopt.like.dto.response;

import org.sopt.post.domain.Post;

public record LikeResponse(
        Long postId,
        int likeCount
) {
    public static LikeResponse from(Post post) {
        return new LikeResponse(post.getId(), post.getLikeCount());
    }
}
