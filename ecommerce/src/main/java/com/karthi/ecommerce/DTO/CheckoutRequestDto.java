package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequestDto(
    @NotBlank(message = "Shipping address is required")
    String shippingAddress,

    String paymentMethod
) {}
