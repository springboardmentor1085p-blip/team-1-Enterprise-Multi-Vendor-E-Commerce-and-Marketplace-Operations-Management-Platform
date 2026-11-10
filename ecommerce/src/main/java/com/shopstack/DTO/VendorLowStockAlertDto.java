package com.shopstack.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorLowStockAlertDto {
    private Long productId;
    private String productName;
    private Long variantId;
    private String sku;
    private String variantName;
    private int availableStock;
    private int reorderLevel;
}

