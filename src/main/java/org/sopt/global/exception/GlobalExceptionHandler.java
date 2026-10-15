package org.sopt.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.global.response.ApiResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("처리되지 않은 예외가 발생했습니다.", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        return build(ErrorCode.INVALID_REQUEST_BODY);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        return build(ErrorCode.INVALID_PARAMETER);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request
    ) {
        ErrorCode errorCode = resolveErrorCode(statusCode);
        return ResponseEntity.status(statusCode).headers(headers).body(ApiResponse.of(errorCode));
    }

    private ResponseEntity<Object> build(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.of(errorCode));
    }

    private ErrorCode resolveErrorCode(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> statusCode.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}
