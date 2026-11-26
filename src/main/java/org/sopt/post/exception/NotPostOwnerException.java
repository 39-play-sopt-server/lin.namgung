package org.sopt.post.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class NotPostOwnerException extends BusinessException {
    public NotPostOwnerException() {
        super(ErrorCode.NOT_POST_OWNER);
    }
}
