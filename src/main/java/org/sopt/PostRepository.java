package org.sopt;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PostRepository {
    private final Map<Long, Post> posts = new HashMap<>();
    private Long sequence = 0L;

    public void save(Post post) {
        sequence++;
        posts.put(sequence, post);
    }

    public List<Post> findAll() {
        return new ArrayList<>(posts.values());
    }

    public Post findById(Long id) {
        return posts.get(id);
    }

    public void deleteById(Long id) {
        posts.remove(id);
    }

    public int count() {
        return posts.size();
    }
}