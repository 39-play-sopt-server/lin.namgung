package org.sopt.post.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class InvalidPageRequestException extends BusinessException {
    public InvalidPageRequestException() {
        super(ErrorCode.INVALID_PAGE_REQUEST);
    }
}
