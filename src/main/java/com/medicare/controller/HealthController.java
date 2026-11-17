package com.medicare.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple controller to confirm the backend is up and reachable.
 *
 * @RestController is shorthand for @Controller + @ResponseBody.
 * It tells Spring:
 *   - this class handles incoming HTTP requests (like @Controller)
 *   - every method's return value should be written directly into the
 *     HTTP response body (usually serialized to JSON), instead of being
 *     treated as the name of a view/template to render.
 */
@RestController
public class HealthController {

    /**
     * @GetMapping("/api/health") maps HTTP GET requests for the path
     * "/api/health" to this method. Spring Boot's Jackson library will
     * automatically convert the returned Map into a JSON object.
     */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "MediCare Backend");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

}
