package org.sopt.auth.dto.request;

public record SignupRequest(
        String email,
        String password,
        String name
) {
}
