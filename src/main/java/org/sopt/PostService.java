package org.sopt;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public void registerPost(String title, String content, String author, Category category) {
        Post post = new Post(title, content, author, category);
        postRepository.save(post);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post getPost(Long id) {
        Post post = findPost(id);
        post.increaseViewCount();
        return post;
    }

    public void changePost(Long id, String title, String content){
        Post post = findPost(id);
        post.update(title, content);
    }

    public void removePost(Long id) {
        findPost(id);
        postRepository.deleteById(id);
    }

    private Post findPost(Long id) {
        Post post = postRepository.findById(id);
        if (post == null) {
            throw new PostNotFoundException("존재하지 않는 게시글입니다.");
        }
        return post;
    }
}