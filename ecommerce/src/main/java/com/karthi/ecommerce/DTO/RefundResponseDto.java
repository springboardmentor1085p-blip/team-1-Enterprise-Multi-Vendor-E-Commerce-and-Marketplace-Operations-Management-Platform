package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.karthi.Enum.RefundStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponseDto {

    private Long refundId;
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private Long orderItemId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private String providerRefundReference;
    private OffsetDateTime requestedAt;
    private OffsetDateTime processedAt;
}
