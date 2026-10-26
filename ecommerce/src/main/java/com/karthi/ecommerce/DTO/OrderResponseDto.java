package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponseDto(
    Long orderId,
    String orderNumber,
    String status,
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal grandTotal,
    String shippingAddress,
    String paymentMethod,
    OffsetDateTime placedAt,
    List<OrderItemResponseDto> items
) {}
