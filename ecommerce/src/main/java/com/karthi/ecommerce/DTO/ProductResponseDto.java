package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Long productId;
    private Long vendorId;
    private String vendorBusinessName;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String name;
    private String description;
    private String status;
    private List<VariantResponseDto> variants;
    private List<String> imageUrls;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantResponseDto {
        private Long variantId;
        private String sku;
        private String variantName;
        private String attributesJson;
        private BigDecimal price;
        private boolean isActive;
        private Integer availableStock;
    }
}
