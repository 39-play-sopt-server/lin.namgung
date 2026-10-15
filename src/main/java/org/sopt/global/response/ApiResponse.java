package org.sopt.global.response;

public record ApiResponse<T>(
        String message,
        T data
) {
}