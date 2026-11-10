package com.karthi.ecommerce.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionResponseDto {
    private Long vendorId;
    private String businessName;
    private BigDecimal commissionRate;
    private long totalOrdersCount;
    private BigDecimal grossSales;
    private BigDecimal platformCommission;
    private BigDecimal netEarnings;
    private List<VendorSaleItemDto> recentSales;
}

