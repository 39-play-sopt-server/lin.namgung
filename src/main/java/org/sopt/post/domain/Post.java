package org.sopt.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.sopt.global.entity.BaseEntity;
import org.sopt.global.exception.ErrorCode;
import org.sopt.post.exception.InvalidPostException;

@Entity
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String author;

    @Enumerated(EnumType.STRING)
    private Category category;

    private int viewCount;

    protected Post() {
    }

    private Post(String title, String content, String author, Category category) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
        this.viewCount = 0;
    }

    public static Post create(String title, String content, String author, Category category) {
        validateTitle(title);
        validateContent(content);
        validateAuthor(author);
        validateCategory(category);
        return new Post(title, content, author, category);
    }

    public Long getId() {
        return id;
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

    public int getViewCount() {
        return viewCount;
    }

    public void update(String title, String content) {
        if (title == null && content == null) {
            throw new InvalidPostException(ErrorCode.EMPTY_UPDATE_REQUEST);
        }
        if (title != null) {
            validateTitle(title);
        }
        if (content != null) {
            validateContent(content);
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public void increaseViewCount() {
        viewCount++;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidPostException(ErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidPostException(ErrorCode.INVALID_CONTENT);
        }
    }

    private static void validateAuthor(String author) {
        if (author == null || author.isBlank()) {
            throw new InvalidPostException(ErrorCode.INVALID_AUTHOR);
        }
    }

    private static void validateCategory(Category category) {
        if (category == null) {
            throw new InvalidPostException(ErrorCode.INVALID_CATEGORY);
        }
    }
}
