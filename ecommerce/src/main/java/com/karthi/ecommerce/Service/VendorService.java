package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.VendorAnalyticsOverviewDto;
import com.karthi.ecommerce.DTO.VendorCommissionResponseDto;
import com.karthi.ecommerce.DTO.VendorLowStockAlertDto;
import com.karthi.ecommerce.DTO.VendorResponseDto;
import com.karthi.ecommerce.DTO.VendorSaleItemDto;
import com.karthi.ecommerce.DTO.VendorTopProductDto;
import com.karthi.ecommerce.Enum.ProductStatus;
import com.karthi.ecommerce.Enum.VendorStatus;
import com.karthi.ecommerce.Model.Inventory;
import com.karthi.ecommerce.Model.OrderItem;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorApproval;
import com.karthi.ecommerce.Repository.InventoryRepository;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorApprovalRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorApprovalRepository vendorApprovalRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public VendorService(
            VendorRepository vendorRepository,
            VendorApprovalRepository vendorApprovalRepository,
            UserRepository userRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorApprovalRepository = vendorApprovalRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // ─── Internal helper: fetch raw Vendor entity (used within service layer only) ───

    @Transactional(readOnly = true)
    public Vendor getVendorEntityById(Long vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with ID: " + vendorId));
    }

    @Transactional(readOnly = true)
    public Vendor getVendorEntityByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return vendorRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No vendor profile associated with account: " + email));
    }

    // ─── Public API methods — return safe DTOs, never raw entities ───

    @Transactional(readOnly = true)
    public List<VendorResponseDto> getAllVendors(VendorStatus status) {
        List<Vendor> vendors = (status != null)
                ? vendorRepository.findByStatus(status)
                : vendorRepository.findAll();
        return vendors.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VendorResponseDto getVendorById(Long vendorId) {
        return toDto(getVendorEntityById(vendorId));
    }

    @Transactional(readOnly = true)
    public VendorResponseDto getVendorByEmail(String email) {
        return toDto(getVendorEntityByEmail(email));
    }

    @Transactional
    public VendorResponseDto updateVendorProfile(String email, String businessName) {
        Vendor vendor = getVendorEntityByEmail(email);
        if (businessName != null && !businessName.isBlank()) {
            vendor.setBusinessName(businessName);
        }
        return toDto(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponseDto updateVendorStatus(Long vendorId, VendorStatus status, String adminEmail, String notes) {
        Vendor vendor = getVendorEntityById(vendorId);
        vendor.setStatus(status);
        Vendor updatedVendor = vendorRepository.save(vendor);

        // Record approval / status decision in vendor_approvals table
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        VendorApproval approval = new VendorApproval();
        approval.setVendor(updatedVendor);
        approval.setReviewedByUser(admin);
        approval.setDecision(status.name());
        approval.setNotes(notes != null ? notes : "Status changed to " + status.name());
        vendorApprovalRepository.save(approval);

        return toDto(updatedVendor);
    }

    @Transactional(readOnly = true)
    public List<VendorApproval> getVendorApprovalHistory(Long vendorId) {
        Vendor vendor = getVendorEntityById(vendorId);
        return vendorApprovalRepository.findByVendor(vendor);
    }

    // ─── Milestone 3: Commission Calculation & Statement ─────────────────────────

    @Transactional
    public VendorResponseDto updateCommissionRate(Long vendorId, BigDecimal commissionRate) {
        Vendor vendor = getVendorEntityById(vendorId);
        vendor.setCommissionRate(commissionRate);
        return toDto(vendorRepository.save(vendor));
    }

    @Transactional(readOnly = true)
    public VendorCommissionResponseDto getVendorCommissionStatement(Long vendorId) {
        Vendor vendor = getVendorEntityById(vendorId);
        return buildCommissionStatement(vendor);
    }

    @Transactional(readOnly = true)
    public VendorCommissionResponseDto getVendorCommissionStatementByEmail(String email) {
        Vendor vendor = getVendorEntityByEmail(email);
        return buildCommissionStatement(vendor);
    }

    @Transactional(readOnly = true)
    public List<VendorCommissionResponseDto> getAllVendorsCommissionSummary() {
        return vendorRepository.findAll().stream()
                .map(this::buildCommissionStatement)
                .collect(Collectors.toList());
    }

    private VendorCommissionResponseDto buildCommissionStatement(Vendor vendor) {
        List<OrderItem> orderItems = orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(vendor.getVendorId());

        BigDecimal commissionRate = vendor.getCommissionRate() != null
                ? vendor.getCommissionRate()
                : new BigDecimal("10.00");

        BigDecimal grossSales = BigDecimal.ZERO;
        Set<Long> uniqueOrders = new HashSet<>();
        List<VendorSaleItemDto> sales = new ArrayList<>();

        for (OrderItem oi : orderItems) {
            grossSales = grossSales.add(oi.getLineTotal());
            if (oi.getOrder() != null) {
                uniqueOrders.add(oi.getOrder().getOrderId());
            }

            BigDecimal itemCommission = oi.getLineTotal()
                    .multiply(commissionRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal itemPayout = oi.getLineTotal().subtract(itemCommission);

            sales.add(VendorSaleItemDto.builder()
                    .orderItemId(oi.getOrderItemId())
                    .orderId(oi.getOrder() != null ? oi.getOrder().getOrderId() : null)
                    .orderNumber(oi.getOrder() != null ? oi.getOrder().getOrderNumber() : "N/A")
                    .orderStatus(oi.getOrder() != null && oi.getOrder().getStatus() != null ? oi.getOrder().getStatus().name() : "PENDING")
                    .productName(oi.getProductNameSnapshot())
                    .sku(oi.getSkuSnapshot())
                    .quantity(oi.getQuantity())
                    .unitPrice(oi.getUnitPrice())
                    .lineTotal(oi.getLineTotal())
                    .commissionRate(commissionRate)
                    .commissionAmount(itemCommission)
                    .vendorPayout(itemPayout)
                    .placedAt(oi.getOrder() != null ? oi.getOrder().getPlacedAt() : null)
                    .build());
        }

        BigDecimal platformCommission = grossSales.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netEarnings = grossSales.subtract(platformCommission);

        return VendorCommissionResponseDto.builder()
                .vendorId(vendor.getVendorId())
                .businessName(vendor.getBusinessName())
                .commissionRate(commissionRate)
                .totalOrdersCount(uniqueOrders.size())
                .grossSales(grossSales)
                .platformCommission(platformCommission)
                .netEarnings(netEarnings)
                .recentSales(sales)
                .build();
    }

    // ─── Milestone 3: Vendor Analytics & Dashboard Overview ──────────────────────

    @Transactional(readOnly = true)
    public VendorAnalyticsOverviewDto getVendorAnalyticsOverview(String email) {
        Vendor vendor = getVendorEntityByEmail(email);
        List<OrderItem> items = orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(vendor.getVendorId());

        BigDecimal commissionRate = vendor.getCommissionRate() != null
                ? vendor.getCommissionRate()
                : new BigDecimal("10.00");

        BigDecimal grossRevenue = BigDecimal.ZERO;
        long totalUnitsSold = 0;
        Set<Long> uniqueOrders = new HashSet<>();

        for (OrderItem oi : items) {
            grossRevenue = grossRevenue.add(oi.getLineTotal());
            totalUnitsSold += oi.getQuantity();
            if (oi.getOrder() != null) {
                uniqueOrders.add(oi.getOrder().getOrderId());
            }
        }

        BigDecimal commissionPaid = grossRevenue.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netEarnings = grossRevenue.subtract(commissionPaid);

        long activeProducts = productRepository.countByVendor_VendorIdAndStatus(vendor.getVendorId(), ProductStatus.APPROVED);
        long pendingProducts = productRepository.countByVendor_VendorIdAndStatus(vendor.getVendorId(), ProductStatus.PENDING_APPROVAL);

        return VendorAnalyticsOverviewDto.builder()
                .vendorId(vendor.getVendorId())
                .businessName(vendor.getBusinessName())
                .totalSalesRevenue(grossRevenue)
                .netEarnings(netEarnings)
                .totalCommissionPaid(commissionPaid)
                .totalUnitsSold(totalUnitsSold)
                .totalOrdersCount(uniqueOrders.size())
                .activeProductsCount(activeProducts)
                .pendingApprovalProductsCount(pendingProducts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VendorTopProductDto> getVendorTopProducts(String email, int limit) {
        Vendor vendor = getVendorEntityByEmail(email);
        List<OrderItem> items = orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(vendor.getVendorId());

        // Group by product name / variant product
        Map<String, VendorTopProductDto> productMap = new HashMap<>();
        for (OrderItem oi : items) {
            String name = oi.getProductNameSnapshot();
            Long prodId = (oi.getVariant() != null && oi.getVariant().getProduct() != null)
                    ? oi.getVariant().getProduct().getProductId()
                    : null;

            productMap.compute(name, (k, v) -> {
                if (v == null) {
                    return VendorTopProductDto.builder()
                            .productId(prodId)
                            .productName(name)
                            .totalQuantitySold(oi.getQuantity())
                            .totalRevenue(oi.getLineTotal())
                            .build();
                } else {
                    v.setTotalQuantitySold(v.getTotalQuantitySold() + oi.getQuantity());
                    v.setTotalRevenue(v.getTotalRevenue().add(oi.getLineTotal()));
                    return v;
                }
            });
        }

        return productMap.values().stream()
                .sorted(Comparator.comparing(VendorTopProductDto::getTotalRevenue).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorLowStockAlertDto> getVendorLowStockAlerts(String email) {
        Vendor vendor = getVendorEntityByEmail(email);
        List<Inventory> lowStock = inventoryRepository.findLowStockInventoriesByVendor(vendor.getVendorId());

        return lowStock.stream().map(inv -> {
            int available = inv.getQuantityOnHand() - inv.getQuantityReserved();
            return VendorLowStockAlertDto.builder()
                    .productId(inv.getVariant().getProduct().getProductId())
                    .productName(inv.getVariant().getProduct().getName())
                    .variantId(inv.getVariant().getVariantId())
                    .sku(inv.getVariant().getSku())
                    .variantName(inv.getVariant().getVariantName())
                    .availableStock(available)
                    .reorderLevel(inv.getReorderLevel())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorSaleItemDto> getVendorOrderItems(String email) {
        Vendor vendor = getVendorEntityByEmail(email);
        BigDecimal commissionRate = vendor.getCommissionRate() != null
                ? vendor.getCommissionRate()
                : new BigDecimal("10.00");

        return orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(vendor.getVendorId()).stream()
                .map(oi -> {
                    BigDecimal itemCommission = oi.getLineTotal()
                            .multiply(commissionRate)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal itemPayout = oi.getLineTotal().subtract(itemCommission);

                    return VendorSaleItemDto.builder()
                            .orderItemId(oi.getOrderItemId())
                            .orderId(oi.getOrder() != null ? oi.getOrder().getOrderId() : null)
                            .orderNumber(oi.getOrder() != null ? oi.getOrder().getOrderNumber() : "N/A")
                            .orderStatus(oi.getOrder() != null && oi.getOrder().getStatus() != null ? oi.getOrder().getStatus().name() : "PENDING")
                            .productName(oi.getProductNameSnapshot())
                            .sku(oi.getSkuSnapshot())
                            .quantity(oi.getQuantity())
                            .unitPrice(oi.getUnitPrice())
                            .lineTotal(oi.getLineTotal())
                            .commissionRate(commissionRate)
                            .commissionAmount(itemCommission)
                            .vendorPayout(itemPayout)
                            .placedAt(oi.getOrder() != null ? oi.getOrder().getPlacedAt() : null)
                            .build();
                }).collect(Collectors.toList());
    }

    // ─── Mapper: Vendor entity → safe VendorResponseDto ───

    private VendorResponseDto toDto(Vendor vendor) {
        User user = vendor.getUser();
        return new VendorResponseDto(
                vendor.getVendorId(),
                vendor.getBusinessName(),
                vendor.getStatus().name(),
                vendor.getCommissionRate() != null ? vendor.getCommissionRate().toPlainString() : null,
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                vendor.getCreatedAt()
        );
    }
}
