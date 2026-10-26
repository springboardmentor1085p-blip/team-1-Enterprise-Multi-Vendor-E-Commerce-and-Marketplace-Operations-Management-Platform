package com.karthi.ecommerce.DTO;

import java.util.List;

public record WishlistResponseDto(
    Long wishlistId,
    String name,
    List<WishlistItemResponseDto> items
) {}
