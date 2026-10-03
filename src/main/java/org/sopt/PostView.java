package org.sopt;

import java.util.List;
import java.util.Scanner;

public class PostView {
	private final Scanner scanner = new Scanner(System.in);

	public int inputCommand() {
		System.out.println("\n=== 게시판 ===");
		System.out.println("1. 게시글 작성");
		System.out.println("2. 게시글 목록 조회");
		System.out.println("3. 게시글 단건 조회");
		System.out.println("4. 게시글 수정");
		System.out.println("5. 게시글 삭제");
		System.out.println("6. 종료");
		return inputNumber("선택: ");
	}

	public String input(String message) {
		System.out.print(message);
		return scanner.nextLine();
	}

	public int inputNumber(String message) {
		return Integer.parseInt(input(message));
	}

	public void printMessage(String message) {
		System.out.println(message);
	}

	public void printPostList(List<Post> posts) {
		System.out.println("\n=== 게시글 목록 ===");

		if (posts.isEmpty()) {
			System.out.println("게시글이 없습니다.");
			return;
		}

		for (int i = 0; i < posts.size(); i++) {
			System.out.println((i + 1) + ". " + posts.get(i).getTitle());
		}
	}

	public void printPost(Post post) {
		System.out.println("\n=== 게시글 ===");
		System.out.println("제목: " + post.getTitle());
		System.out.println("내용: " + post.getContent());
	}
}
