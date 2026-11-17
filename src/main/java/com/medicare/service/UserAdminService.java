package com.medicare.service;

import com.medicare.dto.CreateUserRequestDto;
import com.medicare.dto.ResetPasswordRequestDto;
import com.medicare.dto.UserResponseDto;

import java.util.List;

public interface UserAdminService {

    List<UserResponseDto> getAllUsers();

    UserResponseDto createUser(CreateUserRequestDto dto);

    void resetPassword(Long userId, ResetPasswordRequestDto dto);
}
