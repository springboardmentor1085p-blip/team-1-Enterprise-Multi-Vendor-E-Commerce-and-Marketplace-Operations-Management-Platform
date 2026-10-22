package com.karthi.ecommerce.DTO;

import com.karthi.Enum.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private String comments;
}
