package org.sopt.global.exception;

import org.sopt.global.response.ResponseCode;
import org.springframework.http.HttpStatus;

public enum ErrorCode implements ResponseCode {
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "제목이 비어있습니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "본문이 비어있습니다."),
    INVALID_AUTHOR(HttpStatus.BAD_REQUEST, "작성자가 비어있습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "카테고리가 비어있습니다."),
    EMPTY_UPDATE_REQUEST(HttpStatus.BAD_REQUEST, "수정할 제목 또는 본문을 입력해주세요."),

    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "page는 1 이상, size는 1 이상 100 이하여야 합니다."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "검색어가 비어있습니다."),

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    NOT_POST_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 게시글만 수정, 삭제할 수 있습니다."),

    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 누르지 않은 게시글입니다."),

    INVALID_MEMBER_NAME(HttpStatus.BAD_REQUEST, "회원 이름이 비어있습니다."),
    INVALID_MEMBER_ID(HttpStatus.BAD_REQUEST, "회원 id가 비어있습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상 64자 이하여야 합니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 토큰을 재발급해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 없거나 유효하지 않습니다. 다시 로그인해주세요."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다. 다시 시도해주세요."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "로그인 요청 정보가 일치하지 않습니다. 처음부터 다시 시도해주세요."),
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. JSON 형식과 값을 확인해주세요."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터의 형식이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

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
