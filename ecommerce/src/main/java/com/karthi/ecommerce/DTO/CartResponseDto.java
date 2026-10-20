package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {

    private Long cartId;
    private Long userId;
    private List<CartItemResponseDto> items;
    private BigDecimal totalPrice;
    private Integer totalItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponseDto {
        private Long cartItemId;
        private Long variantId;
        private String sku;
        private String variantName;
        private Long productId;
        private String productName;
        private Long vendorId;
        private String vendorBusinessName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal itemTotal;
    }
}
