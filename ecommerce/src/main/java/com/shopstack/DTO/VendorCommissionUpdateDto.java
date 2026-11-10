package com.shopstack.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionUpdateDto {

    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.00", message = "Commission rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Commission rate cannot exceed 100%")
    private BigDecimal commissionRate;
}

