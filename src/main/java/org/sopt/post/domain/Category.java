package org.sopt.post.domain;

public enum Category {
    NOTICE("공지"),

    QUESTION("질문"),

    STUDY("스터디"),

    FREE("자유");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
