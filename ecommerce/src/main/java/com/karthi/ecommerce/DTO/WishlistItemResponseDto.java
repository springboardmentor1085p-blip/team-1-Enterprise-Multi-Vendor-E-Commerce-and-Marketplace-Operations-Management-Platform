package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WishlistItemResponseDto(
    Long wishlistItemId,
    Long productId,
    String productName,
    BigDecimal price,
    boolean inStock,
    OffsetDateTime addedAt
) {}
