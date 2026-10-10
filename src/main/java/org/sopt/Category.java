package org.sopt;

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

    public static Category fromNumber(int number) {
        Category[] categories = Category.values();
        if (number < 1 || number > categories.length) {
            throw new IllegalArgumentException("존재하지 않는 장르입니다.");
        }
        return categories[number - 1];
    }
}