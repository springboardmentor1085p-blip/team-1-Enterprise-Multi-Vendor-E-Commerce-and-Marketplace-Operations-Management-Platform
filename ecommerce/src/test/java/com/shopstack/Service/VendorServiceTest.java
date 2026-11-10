package com.shopstack.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shopstack.DTO.VendorAnalyticsOverviewDto;
import com.shopstack.DTO.VendorCommissionResponseDto;
import com.shopstack.DTO.VendorLowStockAlertDto;
import com.shopstack.DTO.VendorResponseDto;
import com.shopstack.DTO.VendorSaleItemDto;
import com.shopstack.DTO.VendorTopProductDto;
import com.shopstack.Enum.OrderStatus;
import com.shopstack.Enum.ProductStatus;
import com.shopstack.Enum.VendorStatus;
import com.shopstack.Model.Inventory;
import com.shopstack.Model.Order;
import com.shopstack.Model.OrderItem;
import com.shopstack.Model.Product;
import com.shopstack.Model.ProductVariant;
import com.shopstack.Model.User;
import com.shopstack.Model.Vendor;
import com.shopstack.Repository.InventoryRepository;
import com.shopstack.Repository.OrderItemRepository;
import com.shopstack.Repository.ProductRepository;
import com.shopstack.Repository.UserRepository;
import com.shopstack.Repository.VendorApprovalRepository;
import com.shopstack.Repository.VendorRepository;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorApprovalRepository vendorApprovalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private VendorService vendorService;

    private User vendorUser;
    private Vendor testVendor;
    private Product testProduct;
    private ProductVariant testVariant;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        vendorUser = new User();
        vendorUser.setUserId(10L);
        vendorUser.setEmail("vendor@shopstack.com");
        vendorUser.setFullName("Vendor User");
        vendorUser.setPhone("+1234567890");

        testVendor = new Vendor();
        testVendor.setVendorId(1L);
        testVendor.setUser(vendorUser);
        testVendor.setBusinessName("Acme Electronics");
        testVendor.setStatus(VendorStatus.APPROVED);
        testVendor.setCommissionRate(new BigDecimal("10.00"));
        testVendor.setCreatedAt(OffsetDateTime.now());

        testProduct = new Product();
        testProduct.setProductId(100L);
        testProduct.setName("Wireless Noise-Canceling Headphones");
        testProduct.setVendor(testVendor);
        testProduct.setStatus(ProductStatus.APPROVED);

        testVariant = new ProductVariant();
        testVariant.setVariantId(200L);
        testVariant.setProduct(testProduct);
        testVariant.setSku("HEAD-BLK-01");
        testVariant.setVariantName("Black Edition");
        testVariant.setPrice(new BigDecimal("150.00"));

        testOrder = new Order();
        testOrder.setOrderId(500L);
        testOrder.setOrderNumber("ORD-TEST-12345");
        testOrder.setStatus(OrderStatus.DELIVERED);
        testOrder.setPlacedAt(OffsetDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(1001L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setVendor(testVendor);
        testOrderItem.setVariant(testVariant);
        testOrderItem.setProductNameSnapshot("Wireless Noise-Canceling Headphones");
        testOrderItem.setSkuSnapshot("HEAD-BLK-01");
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(new BigDecimal("150.00"));
        testOrderItem.setLineTotal(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Should update vendor commission rate successfully")
    void testUpdateCommissionRate() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal newRate = new BigDecimal("12.50");
        VendorResponseDto result = vendorService.updateCommissionRate(1L, newRate);

        assertNotNull(result);
        assertEquals("12.50", result.commissionRate());
        verify(vendorRepository).save(testVendor);
    }

    @Test
    @DisplayName("Should accurately calculate vendor commission statement")
    void testGetVendorCommissionStatement() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendor));
        when(orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(1L))
                .thenReturn(List.of(testOrderItem));

        VendorCommissionResponseDto statement = vendorService.getVendorCommissionStatement(1L);

        assertNotNull(statement);
        assertEquals(1L, statement.getVendorId());
        assertEquals("Acme Electronics", statement.getBusinessName());
        assertEquals(1, statement.getTotalOrdersCount());
        // Gross: 300.00, Commission (10%): 30.00, Net: 270.00
        assertEquals(new BigDecimal("300.00"), statement.getGrossSales());
        assertEquals(new BigDecimal("30.00"), statement.getPlatformCommission());
        assertEquals(new BigDecimal("270.00"), statement.getNetEarnings());

        assertEquals(1, statement.getRecentSales().size());
        VendorSaleItemDto item = statement.getRecentSales().get(0);
        assertEquals("ORD-TEST-12345", item.getOrderNumber());
        assertEquals(new BigDecimal("30.00"), item.getCommissionAmount());
        assertEquals(new BigDecimal("270.00"), item.getVendorPayout());
    }

    @Test
    @DisplayName("Should return vendor analytics overview with correct KPI totals")
    void testGetVendorAnalyticsOverview() {
        when(userRepository.findByEmail("vendor@shopstack.com")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.of(testVendor));
        when(orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(1L))
                .thenReturn(List.of(testOrderItem));
        when(productRepository.countByVendor_VendorIdAndStatus(1L, ProductStatus.APPROVED)).thenReturn(5L);
        when(productRepository.countByVendor_VendorIdAndStatus(1L, ProductStatus.PENDING_APPROVAL)).thenReturn(2L);

        VendorAnalyticsOverviewDto overview = vendorService.getVendorAnalyticsOverview("vendor@shopstack.com");

        assertNotNull(overview);
        assertEquals(new BigDecimal("300.00"), overview.getTotalSalesRevenue());
        assertEquals(new BigDecimal("270.00"), overview.getNetEarnings());
        assertEquals(new BigDecimal("30.00"), overview.getTotalCommissionPaid());
        assertEquals(2L, overview.getTotalUnitsSold());
        assertEquals(1L, overview.getTotalOrdersCount());
        assertEquals(5L, overview.getActiveProductsCount());
        assertEquals(2L, overview.getPendingApprovalProductsCount());
    }

    @Test
    @DisplayName("Should return top products ranked by sales revenue")
    void testGetVendorTopProducts() {
        when(userRepository.findByEmail("vendor@shopstack.com")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.of(testVendor));
        when(orderItemRepository.findByVendor_VendorIdOrderByOrder_PlacedAtDesc(1L))
                .thenReturn(List.of(testOrderItem));

        List<VendorTopProductDto> topProducts = vendorService.getVendorTopProducts("vendor@shopstack.com", 5);

        assertNotNull(topProducts);
        assertEquals(1, topProducts.size());
        VendorTopProductDto top = topProducts.get(0);
        assertEquals(100L, top.getProductId());
        assertEquals("Wireless Noise-Canceling Headphones", top.getProductName());
        assertEquals(2L, top.getTotalQuantitySold());
        assertEquals(new BigDecimal("300.00"), top.getTotalRevenue());
    }

    @Test
    @DisplayName("Should detect low-stock inventory alerts for vendor products")
    void testGetVendorLowStockAlerts() {
        when(userRepository.findByEmail("vendor@shopstack.com")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.of(testVendor));

        Inventory lowInv = new Inventory();
        lowInv.setInventoryId(88L);
        lowInv.setVariant(testVariant);
        lowInv.setQuantityOnHand(5);
        lowInv.setQuantityReserved(1); // available = 4
        lowInv.setReorderLevel(10); // 4 <= 10 -> Low stock!

        when(inventoryRepository.findLowStockInventoriesByVendor(1L))
                .thenReturn(List.of(lowInv));

        List<VendorLowStockAlertDto> alerts = vendorService.getVendorLowStockAlerts("vendor@shopstack.com");

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        VendorLowStockAlertDto alert = alerts.get(0);
        assertEquals("HEAD-BLK-01", alert.getSku());
        assertEquals(4, alert.getAvailableStock());
        assertEquals(10, alert.getReorderLevel());
    }
}

