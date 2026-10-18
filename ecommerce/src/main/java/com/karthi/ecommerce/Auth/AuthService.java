package com.karthi.ecommerce.Auth;

import com.karthi.ecommerce.Auth.DTO.AuthResponse;
import com.karthi.ecommerce.Auth.DTO.LoginRequest;
import com.karthi.ecommerce.Auth.DTO.RegisterRequest;
import com.karthi.ecommerce.Auth.DTO.UserResponseDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponseDto getCurrentUser(String email);
}
