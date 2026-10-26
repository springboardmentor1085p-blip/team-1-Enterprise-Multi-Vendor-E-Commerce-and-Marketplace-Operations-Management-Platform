package com.shopstack.Auth;

import com.shopstack.DTO.AuthResponse;
import com.shopstack.DTO.LoginRequest;
import com.shopstack.DTO.RegisterRequest;
import com.shopstack.DTO.UserResponseDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponseDto getCurrentUser(String email);
}
