package org.sopt.global.security;

import jakarta.servlet.http.HttpServletResponse;
import org.sopt.global.exception.ErrorCode;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class SecurityErrorResponder {
    private SecurityErrorResponder() {
    }

    static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"code\":\"" + errorCode.getCode()
                        + "\",\"message\":\"" + errorCode.getMessage()
                        + "\",\"data\":null}"
        );
    }
}
