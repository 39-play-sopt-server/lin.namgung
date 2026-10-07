package org.sopt;

public class PostController {
	private final PostService postService;
	private final PostView postView;

	public PostController(PostView postView, PostService postService) {
		this.postView = postView;
		this.postService = postService;
	}

	public void run() {
		while (true) {
			int command = postView.inputCommand();

			switch (command) {
				case 1 -> createPost();
				case 2 -> postView.printPostList(postService.getAllPosts());
				case 3 -> readPost();
				case 4 -> updatePost();
				case 5 -> deletePost();
				case 6 -> {
					postView.printMessage("프로그램을 종료합니다.");
					return;
				}
				default -> postView.printMessage("잘못된 입력입니다.");
			}
		}
	}

	private void createPost() {
		String title = postView.input("제목: ");
		String content = postView.input("내용: ");

		postService.registerPost(title, content);
		postView.printMessage("게시글이 작성되었습니다.");
	}

	private void readPost() {
		Integer index = selectPostIndex("조회할 게시글 번호: ");
		if (index == null) {
			return;
		}

		postView.printPost(postService.getPost(index));
	}

	private void updatePost() {
		Integer index = selectPostIndex("수정할 게시글 번호: ");
		if (index == null) {
			return;
		}

		String newTitle = postView.input("새로운 제목: ");
		String newContent = postView.input("새로운 내용: ");

		postService.changePost(index, newTitle, newContent);
		postView.printMessage("게시글이 수정되었습니다.");
	}

	private void deletePost() {
		Integer index = selectPostIndex("삭제할 게시글 번호: ");
		if (index == null) {
			return;
		}

		postService.removePost(index);
		postView.printMessage("게시글이 삭제되었습니다.");
	}

	private Integer selectPostIndex(String message) {
		if (postService.countPosts() == 0) {
			postView.printMessage("게시글이 없습니다.");
			return null;
		}

		int index = postView.inputNumber(message) - 1;

		if (index < 0 || index >= postService.countPosts()) {
			postView.printMessage("존재하지 않는 게시글입니다.");
			return null;
		}

		return index;
	}
}
