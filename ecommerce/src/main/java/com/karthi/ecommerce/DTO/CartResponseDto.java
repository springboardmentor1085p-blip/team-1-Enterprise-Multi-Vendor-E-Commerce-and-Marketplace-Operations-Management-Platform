package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDto(
    Long cartId,
    List<CartItemResponseDto> items,
    int totalItems,
    BigDecimal grandTotal
) {}
