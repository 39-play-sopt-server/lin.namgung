package org.sopt.post.repository;

import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Repository
public class PostRepository {
    private final Map<Long, Post> posts = new HashMap<>();
    private Long sequence = 0L;

    public Post save(Post post) {
        sequence++;
        post.assignId(sequence);
        posts.put(sequence, post);
        return post;
    }

    public List<Post> findPage(Category category, long offset, int limit) {
        return filterByCategory(category)
                .sorted(Comparator.comparing(Post::getId).reversed())
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public Post findById(Long id) {
        return posts.get(id);
    }

    public void deleteById(Long id) {
        posts.remove(id);
    }

    public long count(Category category) {
        return filterByCategory(category).count();
    }

    private Stream<Post> filterByCategory(Category category) {
        return posts.values().stream()
                .filter(post -> category == null || post.getCategory() == category);
    }
}
