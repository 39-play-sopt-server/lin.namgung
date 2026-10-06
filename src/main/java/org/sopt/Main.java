package org.sopt;

public class Main {

	public static void main(String[] args) {
		PostView postView = new PostView();
		PostController postController = new PostController(postView);

		postController.run();
	}
}
