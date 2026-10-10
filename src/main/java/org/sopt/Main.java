package org.sopt;

public class Main {

	public static void main(String[] args) {
		PostView postView = new PostView();
		PostRepository postRepository = new PostRepository();
		PostService postService = new PostService(postRepository);
		PostController postController = new PostController(postView, postService);
		postController.run();
	}
}
