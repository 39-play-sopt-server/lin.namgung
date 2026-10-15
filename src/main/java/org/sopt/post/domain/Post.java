package org.sopt.post.domain;

import org.sopt.global.exception.ErrorCode;
import org.sopt.post.exception.InvalidPostException;

import java.time.LocalDateTime;

public class Post {
	private Long id;
	private String title;
	private String content;

	private String author;
	private Category category;
	private LocalDateTime createdAt;
	private int viewCount;

	public Post(String title, String content, String author, Category category) {
		validate(title, content);
		if (author == null || author.isBlank()) {
			throw new InvalidPostException(ErrorCode.INVALID_AUTHOR);
		}
		this.title = title;
		this.content = content;
		this.author = author;
		this.category = category;
		this.createdAt = LocalDateTime.now();
		this.viewCount = 0;
	}


	public Long getId() {
		return id;
	}

	public void assignId(Long id) {
		if (this.id != null) {
			throw new IllegalStateException("이미 id가 부여된 게시글입니다.");
		}
		this.id = id;
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
			throw new InvalidPostException(ErrorCode.INVALID_TITLE);
		}
		if (content == null || content.isBlank()) {
			throw new InvalidPostException(ErrorCode.INVALID_CONTENT);
		}
	}
}