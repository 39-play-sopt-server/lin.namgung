package org.sopt.post.exception;

import org.sopt.global.exception.BusinessException;
import org.sopt.global.exception.ErrorCode;

public class InvalidSearchKeywordException extends BusinessException {
    public InvalidSearchKeywordException() {
        super(ErrorCode.INVALID_SEARCH_KEYWORD);
    }
}
