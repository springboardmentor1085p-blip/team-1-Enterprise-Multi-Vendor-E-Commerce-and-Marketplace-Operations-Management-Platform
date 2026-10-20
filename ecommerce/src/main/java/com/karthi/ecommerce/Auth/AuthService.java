package com.karthi.ecommerce.Auth;

import com.karthi.ecommerce.DTO.AuthResponse;
import com.karthi.ecommerce.DTO.LoginRequest;
import com.karthi.ecommerce.DTO.RegisterRequest;
import com.karthi.ecommerce.DTO.UserResponseDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponseDto getCurrentUser(String email);
}
