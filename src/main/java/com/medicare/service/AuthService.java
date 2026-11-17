package com.medicare.service;

import com.medicare.dto.AuthResponseDto;
import com.medicare.dto.LoginRequestDto;
import com.medicare.dto.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto dto);

    AuthResponseDto login(LoginRequestDto dto);
}
