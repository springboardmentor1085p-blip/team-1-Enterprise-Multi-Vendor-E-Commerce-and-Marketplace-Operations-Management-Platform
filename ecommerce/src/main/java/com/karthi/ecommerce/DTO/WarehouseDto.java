package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {

    private Long warehouseId;

    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @Builder.Default
    private boolean isActive = true;
}
