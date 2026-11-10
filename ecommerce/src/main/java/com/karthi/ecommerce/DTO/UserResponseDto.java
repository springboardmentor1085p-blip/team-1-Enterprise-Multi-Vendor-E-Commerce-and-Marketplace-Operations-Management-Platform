package com.karthi.ecommerce.DTO;

import java.time.LocalDate;
import java.util.List;

public record UserResponseDto(
    Long userId,
    String email,
    String fullName,
    String phone,
    boolean isActive,
    List<String> roles,
    Long vendorId,
    String businessName,
    String vendorStatus,
    LocalDate dateOfBirth
) {}
