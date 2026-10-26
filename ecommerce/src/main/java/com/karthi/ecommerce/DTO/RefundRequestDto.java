package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private Long orderItemId;

    private BigDecimal amount;

    @NotBlank(message = "Refund reason is required")
    private String reason;
}
