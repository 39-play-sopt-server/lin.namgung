package org.sopt.like.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class AlreadyLikedException extends BusinessException {
    public AlreadyLikedException() {
        super(ErrorCode.ALREADY_LIKED);
    }
}
