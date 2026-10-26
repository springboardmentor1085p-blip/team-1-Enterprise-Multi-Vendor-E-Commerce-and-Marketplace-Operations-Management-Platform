package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;

public record ProductResponseDto(
    Long productId,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    boolean isActive
) {}
