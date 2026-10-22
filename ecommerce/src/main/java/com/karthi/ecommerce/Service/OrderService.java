package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.OrderStatus;
import com.karthi.Enum.PaymentStatus;
import com.karthi.ecommerce.DTO.AddressResponseDto;
import com.karthi.ecommerce.DTO.CheckoutRequestDto;
import com.karthi.ecommerce.DTO.OrderResponseDto;
import com.karthi.ecommerce.DTO.OrderStatusUpdateDto;
import com.karthi.ecommerce.Model.Address;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.OrderItem;
import com.karthi.ecommerce.Model.OrderStatusHistory;
import com.karthi.ecommerce.Model.Payment;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.AddressRepository;
import com.karthi.ecommerce.Repository.CartItemRepository;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.OrderStatusHistoryRepository;
import com.karthi.ecommerce.Repository.PaymentRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusHistoryRepository statusHistoryRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                        UserRepository userRepository,
                        PaymentRepository paymentRepository,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OrderResponseDto checkout(Long userId, CheckoutRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Address address = addressRepository.findById(dto.getShippingAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Shipping address not found: " + dto.getShippingAddressId()));

        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout an empty cart");
        }

        // Verify stock availability
        for (CartItem item : cartItems) {
            Integer available = inventoryService.getAvailableStock(item.getVariant().getVariantId());
            if (available < item.getQuantity()) {
                throw new IllegalArgumentException("Item " + item.getVariant().getProduct().getName() +
                        " (" + item.getVariant().getSku() + ") has only " + available + " in stock");
            }
        }

        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal lineTotal = item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05")); // 5% tax
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("500")) >= 0 ? BigDecimal.ZERO : new BigDecimal("50");
        BigDecimal grandTotal = subtotal.subtract(discount).add(tax).add(shippingFee);

        // Create Order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(address);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTax(tax);
        order.setShippingFee(shippingFee);
        order.setGrandTotal(grandTotal);

        Order savedOrder = orderRepository.save(order);

        // Create Order Items and reserve inventory
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setVariant(item.getVariant());
            oi.setVendor(item.getVariant().getProduct().getVendor());
            oi.setProductNameSnapshot(item.getVariant().getProduct().getName());
            oi.setSkuSnapshot(item.getVariant().getSku());
            oi.setQuantity(item.getQuantity());
            oi.setUnitPrice(item.getVariant().getPrice());
            oi.setLineTotal(item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItems.add(orderItemRepository.save(oi));

            // Reserve stock
            inventoryService.reserveStock(item.getVariant().getVariantId(), item.getQuantity(), savedOrder.getOrderId(), user);
        }

        // Record Order Status History
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setChangedByUser(user);
        history.setOldStatus(null);
        history.setNewStatus(OrderStatus.PENDING);
        history.setComments("Order placed via checkout");
        statusHistoryRepository.save(history);

        // Create Pending Payment Record
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setProvider(dto.getPaymentProvider() != null ? dto.getPaymentProvider() : "MOCK_GATEWAY");
        payment.setProviderReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setAmount(grandTotal);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Clear cart
        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        return mapToDto(savedOrder, orderItems, address, payment.getStatus().name());
    }

    public List<OrderResponseDto> getUserOrders(Long userId) {
        return orderRepository.findByUser_UserIdOrderByPlacedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrderResponseDto getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return mapToDto(order);
    }

    public OrderResponseDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return mapToDto(order);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, Long changedByUserId, OrderStatusUpdateDto dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = dto.getStatus();

        if (oldStatus == newStatus) {
            return mapToDto(order);
        }

        User changedByUser = changedByUserId != null ? userRepository.findById(changedByUserId).orElse(null) : null;

        // If order is cancelled, release reserved stock
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            List<OrderItem> items = orderItemRepository.findByOrder_OrderId(orderId);
            for (OrderItem oi : items) {
                inventoryService.releaseStock(oi.getVariant().getVariantId(), oi.getQuantity(), orderId, changedByUser);
            }
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setChangedByUser(changedByUser);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setComments(dto.getComments());
        statusHistoryRepository.save(history);

        return mapToDto(updatedOrder);
    }

    public OrderResponseDto mapToDto(Order o) {
        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(o.getOrderId());
        List<Payment> payments = paymentRepository.findByOrder_OrderId(o.getOrderId());
        String paymentStatus = payments.isEmpty() ? "PENDING" : payments.get(0).getStatus().name();
        return mapToDto(o, items, o.getShippingAddress(), paymentStatus);
    }

    private OrderResponseDto mapToDto(Order o, List<OrderItem> items, Address addr, String paymentStatus) {
        AddressResponseDto addrDto = AddressResponseDto.builder()
                .addressId(addr.getAddressId())
                .userId(addr.getUser().getUserId())
                .recipientName(addr.getRecipientName())
                .phone(addr.getPhone())
                .addressLine(addr.getAddressLine())
                .city(addr.getCity())
                .region(addr.getRegion())
                .postalCode(addr.getPostalCode())
                .countryCode(addr.getCountryCode())
                .isDefault(addr.isDefault())
                .build();

        List<OrderResponseDto.OrderItemResponseDto> itemDtos = items.stream().map(oi ->
                OrderResponseDto.OrderItemResponseDto.builder()
                        .orderItemId(oi.getOrderItemId())
                        .variantId(oi.getVariant().getVariantId())
                        .vendorId(oi.getVendor().getVendorId())
                        .vendorBusinessName(oi.getVendor().getBusinessName())
                        .productNameSnapshot(oi.getProductNameSnapshot())
                        .skuSnapshot(oi.getSkuSnapshot())
                        .quantity(oi.getQuantity())
                        .unitPrice(oi.getUnitPrice())
                        .lineTotal(oi.getLineTotal())
                        .build()
        ).collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .userId(o.getUser().getUserId())
                .status(o.getStatus())
                .subtotal(o.getSubtotal())
                .discount(o.getDiscount())
                .tax(o.getTax())
                .shippingFee(o.getShippingFee())
                .grandTotal(o.getGrandTotal())
                .shippingAddress(addrDto)
                .items(itemDtos)
                .placedAt(o.getPlacedAt())
                .paymentStatus(paymentStatus)
                .build();
    }
}
