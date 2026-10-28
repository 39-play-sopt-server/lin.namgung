package org.sopt.member.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class InvalidMemberException extends BusinessException {
    public InvalidMemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
