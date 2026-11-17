package com.medicare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.medicare.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * WHY THIS CLASS IS NEEDED:
 * Our GlobalExceptionHandler (@RestControllerAdvice) only catches
 * exceptions thrown while a request is being handled INSIDE Spring
 * MVC - i.e. inside a @Controller method. But authentication failures
 * (no token, or an invalid one) are detected earlier, inside Spring
 * Security's own filter chain, BEFORE the request ever reaches a
 * controller. That means GlobalExceptionHandler never even sees them.
 *
 * AuthenticationEntryPoint is the specific hook Spring Security gives
 * us to customize what happens at exactly that point: "someone tried
 * to access a protected endpoint without valid credentials." We use
 * it here to produce the SAME ApiError JSON shape our other error
 * responses use, instead of Spring Security's plain-text default.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Spring MVC's own JSON writer prints dates as ISO strings
    // ("2026-10-22T09:25:31"); a bare ObjectMapper prints them as number
    // arrays ([2026,10,22,...]). Turning that off keeps 401/403 responses
    // in the same format as every other error the API returns.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication is required or your token is missing/invalid"
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
