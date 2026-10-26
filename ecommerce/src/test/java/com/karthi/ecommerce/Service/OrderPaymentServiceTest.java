package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.karthi.Enum.OrderStatus;
import com.karthi.Enum.PaymentStatus;
import com.karthi.Enum.RefundStatus;
import com.karthi.ecommerce.DTO.OrderStatusUpdateDto;
import com.karthi.ecommerce.DTO.PaymentRequestDto;
import com.karthi.ecommerce.DTO.PaymentResponseDto;
import com.karthi.ecommerce.DTO.RefundRequestDto;
import com.karthi.ecommerce.DTO.RefundResponseDto;
import com.karthi.ecommerce.Model.Address;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.OrderItem;
import com.karthi.ecommerce.Model.Payment;
import com.karthi.ecommerce.Model.ProductVariant;
import com.karthi.ecommerce.Model.Refund;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.AddressRepository;
import com.karthi.ecommerce.Repository.CartItemRepository;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.OrderStatusHistoryRepository;
import com.karthi.ecommerce.Repository.PaymentRepository;
import com.karthi.ecommerce.Repository.RefundRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class OrderPaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private RefundService refundService;
    @Mock
    private RefundRepository refundRepository;

    @Mock
    private OrderService mockOrderService;

    private OrderService orderService;
    private PaymentService paymentService;

    private User testUser;
    private Address testAddress;
    private Order testOrder;
    private ProductVariant testVariant;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, orderItemRepository, statusHistoryRepository,
                cartRepository, cartItemRepository, addressRepository,
                userRepository, paymentRepository, inventoryService, refundService
        );

        paymentService = new PaymentService(
                paymentRepository, orderRepository, mockOrderService
        );

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("customer@test.com");
        testUser.setFullName("John Customer");

        testAddress = new Address();
        testAddress.setAddressId(10L);
        testAddress.setUser(testUser);
        testAddress.setRecipientName("John Customer");
        testAddress.setAddressLine("123 Street");
        testAddress.setCity("Chennai");
        testAddress.setPostalCode("600001");

        testOrder = new Order();
        testOrder.setOrderId(100L);
        testOrder.setUser(testUser);
        testOrder.setOrderNumber("ORD-TEST1234");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setShippingAddress(testAddress);
        testOrder.setSubtotal(new BigDecimal("1000.00"));
        testOrder.setGrandTotal(new BigDecimal("1050.00"));
        testOrder.setPlacedAt(OffsetDateTime.now());

        testVariant = new ProductVariant();
        testVariant.setVariantId(50L);
        testVariant.setSku("SKU-TEST");
        testVariant.setPrice(new BigDecimal("500.00"));

        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(200L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setVariant(testVariant);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(new BigDecimal("500.00"));
        testOrderItem.setLineTotal(new BigDecimal("1000.00"));
        testOrderItem.setProductNameSnapshot("Test Product");
        testOrderItem.setSkuSnapshot("SKU-TEST");
    }

    // ─── 1. State Machine Transitions ──────────────────────────────────────────

    @Test
    void testValidStatusTransition_PendingToConfirmed() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderStatusUpdateDto dto = new OrderStatusUpdateDto();
        dto.setStatus(OrderStatus.CONFIRMED);
        dto.setComments("Payment received");

        var response = orderService.updateOrderStatus(100L, 1L, dto);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    void testInvalidStatusTransition_DeliveredToPending_ThrowsException() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        OrderStatusUpdateDto dto = new OrderStatusUpdateDto();
        dto.setStatus(OrderStatus.PENDING);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(100L, 1L, dto)
        );
    }

    @Test
    void testInvalidStatusTransition_CancelledToConfirmed_ThrowsException() {
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        OrderStatusUpdateDto dto = new OrderStatusUpdateDto();
        dto.setStatus(OrderStatus.CONFIRMED);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(100L, 1L, dto)
        );
    }

    // ─── 2. Cancellation Rules ──────────────────────────────────────────────────

    @Test
    void testCancelOrder_PendingOrder_SuccessAndReleasesStock() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderItemRepository.findByOrder_OrderId(100L)).thenReturn(List.of(testOrderItem));
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        var response = orderService.cancelOrder(100L, 1L, false, "Changed my mind");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryService, times(1)).releaseStock(testVariant.getVariantId(), 2, 100L, testUser);
    }

    @Test
    void testCancelOrder_ShippedOrder_ThrowsException() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalStateException.class, () ->
                orderService.cancelOrder(100L, 1L, false, "Too late")
        );
    }

    @Test
    void testCancelOrder_EmptyReason_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                orderService.cancelOrder(100L, 1L, false, "   ")
        );
    }

    @Test
    void testCancelOrder_UnauthorizedUser_ThrowsException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class, () ->
                orderService.cancelOrder(100L, 999L, false, "Unauthorized cancellation")
        );
    }

    // ─── 3. Payment Sandbox & Idempotency ───────────────────────────────────────

    @Test
    void testPayment_SuccessOutcome() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED)).thenReturn(Optional.empty());
        when(paymentRepository.findByOrder_OrderId(100L)).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(100L);
        dto.setProvider("STRIPE");
        dto.setAmount(new BigDecimal("1050.00"));
        dto.setSandboxOutcome("SUCCESS");

        PaymentResponseDto response = paymentService.processPayment(1L, dto);

        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        verify(mockOrderService, times(1)).confirmPaidOrder(org.mockito.ArgumentMatchers.eq(100L), any());
    }

    @Test
    void testPayment_DeclinedOutcome() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED)).thenReturn(Optional.empty());
        when(paymentRepository.findByOrder_OrderId(100L)).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(100L);
        dto.setProvider("STRIPE");
        dto.setAmount(new BigDecimal("1050.00"));
        dto.setSandboxOutcome("DECLINED");

        PaymentResponseDto response = paymentService.processPayment(1L, dto);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getFailureReason());
        verify(mockOrderService, never()).confirmPaidOrder(any(), any());
    }

    @Test
    void testPayment_DuplicatePaymentRejection() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        Payment existingPayment = new Payment();
        existingPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED))
                .thenReturn(Optional.of(existingPayment));

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(100L);
        dto.setProvider("STRIPE");
        dto.setAmount(new BigDecimal("1050.00"));

        assertThrows(IllegalStateException.class, () ->
                paymentService.processPayment(1L, dto)
        );
    }

    @Test
    void testPayment_IdempotencyKeyReturnsExisting() {
        Payment existing = new Payment();
        existing.setPaymentId(555L);
        existing.setOrder(testOrder);
        existing.setStatus(PaymentStatus.COMPLETED);
        existing.setIdempotencyKey("KEY-123");

        when(paymentRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(existing));

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(100L);
        dto.setIdempotencyKey("KEY-123");

        PaymentResponseDto response = paymentService.processPayment(1L, dto);
        assertEquals(555L, response.getPaymentId());
        verify(orderRepository, never()).findById(any());
    }

    // ─── 4. Refund Coordination ────────────────────────────────────────────────

    @Test
    void testRefundRequest_Success() {
        RefundService realRefundService = new RefundService(
                refundRepository, paymentRepository, orderRepository, orderItemRepository
        );

        Payment completedPayment = new Payment();
        completedPayment.setPaymentId(10L);
        completedPayment.setOrder(testOrder);
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        completedPayment.setAmount(new BigDecimal("1050.00"));

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED))
                .thenReturn(Optional.of(completedPayment));
        when(refundRepository.findByPayment_PaymentIdAndStatusNot(10L, RefundStatus.REJECTED))
                .thenReturn(Optional.empty());
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
            Refund r = i.getArgument(0);
            r.setRefundId(999L);
            return r;
        });

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderId(100L);
        dto.setReason("Item defective");
        dto.setAmount(new BigDecimal("1050.00"));

        RefundResponseDto response = realRefundService.requestRefund(1L, dto);

        assertEquals(999L, response.getRefundId());
        assertEquals(RefundStatus.PENDING, response.getStatus());
        assertEquals("Item defective", response.getReason());
    }

    @Test
    void testRefundRequest_UnpaidOrder_ThrowsException() {
        RefundService realRefundService = new RefundService(
                refundRepository, paymentRepository, orderRepository, orderItemRepository
        );

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrder_OrderIdAndStatus(100L, PaymentStatus.COMPLETED))
                .thenReturn(Optional.empty());

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderId(100L);
        dto.setReason("Item defective");

        assertThrows(IllegalStateException.class, () ->
                realRefundService.requestRefund(1L, dto)
        );
    }
}
