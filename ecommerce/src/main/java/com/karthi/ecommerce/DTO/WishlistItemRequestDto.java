package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotNull;

public record WishlistItemRequestDto(
    @NotNull(message = "Product ID is required")
    Long productId
) {}
