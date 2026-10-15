package org.sopt.global.exception;

import org.sopt.global.response.ResponseCode;
import org.springframework.http.HttpStatus;

public enum ErrorCode implements ResponseCode {
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "제목이 비어있습니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "본문이 비어있습니다."),
    INVALID_AUTHOR(HttpStatus.BAD_REQUEST, "작성자가 비어있습니다."),

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
