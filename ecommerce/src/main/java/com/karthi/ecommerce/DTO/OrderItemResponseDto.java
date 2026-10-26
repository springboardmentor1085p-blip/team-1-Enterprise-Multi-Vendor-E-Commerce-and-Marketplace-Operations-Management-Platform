package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;

public record OrderItemResponseDto(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal
) {}
