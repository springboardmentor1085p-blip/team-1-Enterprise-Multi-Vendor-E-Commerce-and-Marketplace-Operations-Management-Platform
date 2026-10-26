package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Provider is required")
    private String provider; // STRIPE, RAZORPAY, CASH_ON_DELIVERY, MOCK_GATEWAY

    private String providerReference;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String currency = "INR";

    // Sandbox simulation outcomes: "SUCCESS" (default), "DECLINED", "TIMEOUT", "INSUFFICIENT_FUNDS"
    private String sandboxOutcome = "SUCCESS";

    // Unique client idempotency key to prevent double charging
    private String idempotencyKey;
}
