package com.shopstack.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequestDto {

    @NotNull(message = "Shipping address ID is required")
    private Long shippingAddressId;

    private String paymentProvider = "MOCK_GATEWAY"; // e.g. STRIPE, RAZORPAY, PAYPAL, CASH_ON_DELIVERY, MOCK_GATEWAY
}
