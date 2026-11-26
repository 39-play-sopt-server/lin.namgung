package org.sopt.global.response;

import org.springframework.http.HttpStatus;

public enum SuccessCode implements ResponseCode {
    POST_CREATED(HttpStatus.CREATED, "게시글이 작성되었습니다."),
    POST_READ(HttpStatus.OK, "게시글 조회에 성공했습니다."),
    POSTS_READ(HttpStatus.OK, "게시글 목록 조회에 성공했습니다."),
    POST_UPDATED(HttpStatus.OK, "게시글이 수정되었습니다."),
    POST_DELETED(HttpStatus.OK, "게시글이 삭제되었습니다."),
    POST_LIKED(HttpStatus.CREATED, "좋아요를 눌렀습니다."),
    POST_UNLIKED(HttpStatus.OK, "좋아요를 취소했습니다."),
    MEMBER_CREATED(HttpStatus.CREATED, "회원가입이 완료되었습니다."),
    MEMBER_READ(HttpStatus.OK, "내 정보 조회에 성공했습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다."),
    TOKEN_REISSUED(HttpStatus.OK, "토큰이 재발급되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃되었습니다."),
    MEMBER_DELETED(HttpStatus.OK, "회원 탈퇴가 완료되었습니다.");

    private final HttpStatus status;
    private final String message;

    SuccessCode(HttpStatus status, String message) {
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
