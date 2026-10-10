package org.sopt;

import java.time.LocalDateTime;

public class Post {
	private String title;
	private String content;

	private String author;
	private Category category;
	private LocalDateTime createdAt;
	private int viewCount;

	public Post(String title, String content, String author, Category category) {
		validate(title, content);
		if (author == null || author.isBlank()) {
			throw new IllegalArgumentException("작성자가 비어있습니다.");
		}
		this.title = title;
		this.content = content;
		this.author = author;
		this.category = category;
		this.createdAt = LocalDateTime.now();
		this.viewCount = 0;
	}


	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public String getAuthor() {
		return author;
	}

	public Category getCategory() {
		return category;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public int getViewCount() {
		return viewCount;
	}

	public void update(String title, String content) {
		validate(title, content);
		this.title = title;
		this.content = content;
	}

	public void increaseViewCount() {
		viewCount++;
	}

	private void validate(String title, String content){
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("제목이 비어있습니다.");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("본문이 비어있습니다.");
		}
	}
}