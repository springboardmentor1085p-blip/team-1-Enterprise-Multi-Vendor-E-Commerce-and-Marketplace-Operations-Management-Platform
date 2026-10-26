package com.shopstack.DTO;

import com.shopstack.Enum.ProductStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusUpdateDto {

    @NotNull(message = "Status is required (e.g. APPROVED, REJECTED)")
    private ProductStatus status;

    private String notes;
}
