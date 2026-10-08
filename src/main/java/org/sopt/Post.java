package org.sopt;

public class Post {
	private String title;
	private String content;

	public Post(String title, String content) {
		validate(title, content);
		this.title = title;
		this.content = content;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public void update(String title, String content) {
		validate(title, content);
		this.title = title;
		this.content = content;
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