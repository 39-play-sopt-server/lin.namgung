package org.sopt;

import java.util.List;

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

    public Post getPost(int index) {
        validateIndex(index);
        Post post = postRepository.findByIndex(index);
        post.increaseViewCount();
        return post;
    }

    public void changePost(int index, String title, String content){
        validateIndex(index);
        Post post = postRepository.findByIndex(index);
        post.update(title, content);
    }

    public void removePost(int index) {
        validateIndex(index);
        postRepository.deleteByIndex(index);
    }

    public int countPosts() {
        return postRepository.count();
    }

    private void validateIndex(int index) {
        if ( index < 0 || index >= postRepository.count()) {
            throw new PostNotFoundException("존재하지 않는 게시글입니다.");
        }
    }
}