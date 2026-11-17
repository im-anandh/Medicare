package com.medicare.controller;

import com.medicare.dto.CreateUserRequestDto;
import com.medicare.dto.ResetPasswordRequestDto;
import com.medicare.dto.UserResponseDto;
import com.medicare.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account management. Access is set in SecurityConfig:
 *   GET   /api/users               ADMIN
 *   POST  /api/users               ADMIN, RECEPTIONIST (receptionist: patients only - enforced in the service)
 *   PATCH /api/users/{id}/password ADMIN
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userAdminService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.createUser(dto));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @Valid @RequestBody ResetPasswordRequestDto dto) {
        userAdminService.resetPassword(id, dto);
        return ResponseEntity.noContent().build();
    }
}
