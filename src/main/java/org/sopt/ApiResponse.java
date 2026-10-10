package org.sopt;

public record ApiResponse<T>(
        String message,
        T data
) {
}