package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.karthi.Enum.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String providerReference;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String failureReason;
    private String sandboxOutcome;
    private String idempotencyKey;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
