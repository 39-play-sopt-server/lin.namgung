package org.sopt;

import java.util.ArrayList;
import java.util.List;

public class PostController {
	private final List<Post> posts = new ArrayList<>();
	private final PostView postView;

	public PostController(PostView postView) {
		this.postView = postView;
	}

	public void run() {
		while (true) {
			int command = postView.inputCommand();

			switch (command) {
				case 1 -> createPost();
				case 2 -> postView.printPostList(posts);
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

		posts.add(new Post(title, content));
		postView.printMessage("게시글이 작성되었습니다.");
	}

	private void readPost() {
		Integer index = selectPostIndex("조회할 게시글 번호: ");
		if (index == null) {
			return;
		}

		postView.printPost(posts.get(index));
	}

	private void updatePost() {
		Integer index = selectPostIndex("수정할 게시글 번호: ");
		if (index == null) {
			return;
		}

		String newTitle = postView.input("새로운 제목: ");
		String newContent = postView.input("새로운 내용: ");

		posts.get(index).update(newTitle, newContent);
		postView.printMessage("게시글이 수정되었습니다.");
	}

	private void deletePost() {
		Integer index = selectPostIndex("삭제할 게시글 번호: ");
		if (index == null) {
			return;
		}

		posts.remove((int) index);
		postView.printMessage("게시글이 삭제되었습니다.");
	}

	private Integer selectPostIndex(String message) {
		if (posts.isEmpty()) {
			postView.printMessage("게시글이 없습니다.");
			return null;
		}

		int index = postView.inputNumber(message) - 1;

		if (index < 0 || index >= posts.size()) {
			postView.printMessage("존재하지 않는 게시글입니다.");
			return null;
		}

		return index;
	}
}
