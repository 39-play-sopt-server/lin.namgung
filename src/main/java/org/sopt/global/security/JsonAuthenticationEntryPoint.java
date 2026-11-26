package org.sopt.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sopt.auth.jwt.JwtAuthenticationFilter;
import org.sopt.global.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object errorCode = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (errorCode instanceof ErrorCode code) {
            SecurityErrorResponder.write(response, code);
            return;
        }
        SecurityErrorResponder.write(response, ErrorCode.UNAUTHORIZED);
    }
}
