package org.sopt.global.response;

import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> of(ResponseCode responseCode, T data) {
        return new ApiResponse<>(
                responseCode.getStatus().is2xxSuccessful(),
                responseCode.getCode(),
                responseCode.getMessage(),
                data
        );
    }

    public static ApiResponse<Void> of(ResponseCode responseCode) {
        return of(responseCode, null);
    }

    public static <T> ResponseEntity<ApiResponse<T>> toResponseEntity(ResponseCode responseCode, T data) {
        return ResponseEntity.status(responseCode.getStatus()).body(of(responseCode, data));
    }
}
