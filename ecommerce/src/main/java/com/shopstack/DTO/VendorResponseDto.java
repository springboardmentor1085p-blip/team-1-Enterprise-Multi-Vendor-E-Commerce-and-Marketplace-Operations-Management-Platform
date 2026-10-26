package com.shopstack.DTO;

import java.time.OffsetDateTime;

/**
 * Safe DTO for vendor responses — never exposes sensitive User fields like passwordHash.
 */
public record VendorResponseDto(
        Long vendorId,
        String businessName,
        String status,
        String commissionRate,
        // Safe user info only
        Long userId,
        String userEmail,
        String userFullName,
        String userPhone,
        OffsetDateTime createdAt
) {}
