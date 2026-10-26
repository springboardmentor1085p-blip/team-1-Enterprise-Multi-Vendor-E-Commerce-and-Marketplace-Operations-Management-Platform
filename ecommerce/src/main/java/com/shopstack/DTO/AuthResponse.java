package com.shopstack.DTO;

import java.util.List;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String email,
    String fullName,
    List<String> roles,
    Long expiresIn
) {
    public AuthResponse(String token, Long userId, String email, String fullName, List<String> roles, Long expiresIn) {
        this(token, "Bearer", userId, email, fullName, roles, expiresIn);
    }
}
