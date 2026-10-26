package com.shopstack.DTO;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequestDto {

    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    private String description;
    private Long categoryId;
    private Long brandId;
    private List<String> imageUrls;

    @Valid
    private List<VariantUpdateRequestDto> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantUpdateRequestDto {
        private Long variantId;
        private String sku;
        private String variantName;
        private String attributesJson;

        @PositiveOrZero(message = "Variant price cannot be negative")
        private BigDecimal price;

        private Boolean isActive;

        @Min(value = 0, message = "Stock quantity cannot be negative")
        private Integer stockQuantity;

        private Long warehouseId;
    }
}

