package com.medicare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.medicare.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * The 403 counterpart to RestAuthenticationEntryPoint - same idea,
 * different scenario. This fires when a request DOES have a valid,
 * authenticated user, but that user's role isn't allowed to access
 * the requested endpoint (e.g. a logged-in PATIENT calling
 * POST /api/doctors, which SecurityConfig restricts to ADMIN only).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    // Spring MVC's own JSON writer prints dates as ISO strings
    // ("2026-10-22T09:25:31"); a bare ObjectMapper prints them as number
    // arrays ([2026,10,22,...]). Turning that off keeps 401/403 responses
    // in the same format as every other error the API returns.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "You do not have permission to perform this action"
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
