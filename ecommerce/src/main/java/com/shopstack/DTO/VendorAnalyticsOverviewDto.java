package com.shopstack.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAnalyticsOverviewDto {
    private Long vendorId;
    private String businessName;
    private BigDecimal totalSalesRevenue;
    private BigDecimal netEarnings;
    private BigDecimal totalCommissionPaid;
    private long totalUnitsSold;
    private long totalOrdersCount;
    private long activeProductsCount;
    private long pendingApprovalProductsCount;
}

