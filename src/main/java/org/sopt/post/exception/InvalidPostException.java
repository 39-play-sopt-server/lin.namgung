package org.sopt.post.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class InvalidPostException extends BusinessException {
    public InvalidPostException(ErrorCode errorCode) {
        super(errorCode);
    }
}
