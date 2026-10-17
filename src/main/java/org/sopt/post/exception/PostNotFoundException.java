package org.sopt.post.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class PostNotFoundException extends BusinessException {
    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND);
    }
}
