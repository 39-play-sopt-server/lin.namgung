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
    MEMBER_CREATED(HttpStatus.CREATED, "회원이 등록되었습니다."),
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
