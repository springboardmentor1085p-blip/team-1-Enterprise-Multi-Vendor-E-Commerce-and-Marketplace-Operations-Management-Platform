package com.shopstack.DTO;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long brandId;

    private List<VariantRequestDto> variants;

    private List<String> imageUrls;

    @Data
    public static class VariantRequestDto {
        @NotBlank(message = "SKU is required")
        private String sku;

        private String variantName;

        private String attributesJson;

        @NotNull(message = "Price is required")
        private BigDecimal price;

        private Integer initialStock;

        private Long warehouseId;
    }
}
