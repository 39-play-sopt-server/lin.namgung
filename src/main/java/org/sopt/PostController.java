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
		String author = postView.input("작성자: ");
		int categoryNumber = postView.inputNumber("카테고리 (1.공지 2.질문 3.스터디 4.자유): ");

		try {
			Category category = Category.fromNumber(categoryNumber);
			postService.registerPost(title, content, author, category);
			postView.printMessage("게시글이 작성되었습니다.");
		} catch (IllegalArgumentException e) {
			postView.printMessage(e.getMessage());
		}
	}

	private void readPost() {
		int index = selectPostIndex("조회할 게시글 번호: ");

		try {
			postView.printPost(postService.getPost(index));
		} catch (PostNotFoundException e) {
			postView.printMessage(e.getMessage());
		}

	}

	private void updatePost() {
		int index = selectPostIndex("수정할 게시글 번호: ");

		String newTitle = postView.input("새로운 제목: ");
		String newContent = postView.input("새로운 내용: ");

		try {
			postService.changePost(index, newTitle, newContent);
			postView.printMessage("게시글이 수정되었습니다.");
		} catch (PostNotFoundException | IllegalArgumentException e) {
			postView.printMessage(e.getMessage());
		}
	}

	private void deletePost() {
		int index = selectPostIndex("삭제할 게시글 번호: ");

		try {
			postService.removePost(index);
			postView.printMessage("게시글이 삭제되었습니다.");
		} catch (PostNotFoundException e) {
			postView.printMessage(e.getMessage());
		}
	}

	private int selectPostIndex(String message) {
		return postView.inputNumber(message) - 1 ;
	}
}
