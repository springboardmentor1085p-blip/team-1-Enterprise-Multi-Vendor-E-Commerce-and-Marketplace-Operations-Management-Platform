package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCancelRequestDto {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
