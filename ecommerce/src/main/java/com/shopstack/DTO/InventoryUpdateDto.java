package com.shopstack.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryUpdateDto {

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @NotNull(message = "Quantity delta is required")
    private Integer quantityDelta;

    private Integer reorderLevel;

    private String reason; // "RESTOCK", "DAMAGE", "AUDIT"
}
