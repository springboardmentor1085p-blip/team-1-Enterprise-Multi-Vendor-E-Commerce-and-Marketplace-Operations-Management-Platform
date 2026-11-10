package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSaleItemDto {
    private Long orderItemId;
    private Long orderId;
    private String orderNumber;
    private String orderStatus;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal vendorPayout;
    private OffsetDateTime placedAt;
}

