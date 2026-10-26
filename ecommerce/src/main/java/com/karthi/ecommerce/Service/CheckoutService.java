package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.CheckoutRequestDto;
import com.karthi.ecommerce.DTO.OrderItemResponseDto;
import com.karthi.ecommerce.DTO.OrderResponseDto;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.OrderItem;
import com.karthi.ecommerce.Model.OrderStatus;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public CheckoutService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Executes the checkout process atomically within a database transaction:
     * 1. Validates the authenticated user and cart (rejects empty carts).
     * 2. Re-validates product active status and stock availability.
     * 3. Calculates authoritative server-side prices and totals.
     * 4. Deducts purchased quantities from product inventory.
     * 5. Creates Order and OrderItem records preserving purchase-time price snapshots.
     * 6. Clears the user's cart upon success.
     * Any failure rolls back the entire transaction automatically.
     */
    @Transactional
    public OrderResponseDto checkout(String userEmail, CheckoutRequestDto request) {
        if (request.shippingAddress() == null || request.shippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required for checkout");
        }

        User user = getUserByEmail(userEmail);

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cannot checkout: cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout: your shopping cart is empty");
        }

        // Create the Order header
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setShippingAddress(request.shippingAddress().trim());
        order.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod().trim() : "COD");

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // Revalidate stock, snapshot price, and decrease inventory
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + cartItem.getProduct().getProductId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is currently inactive and cannot be ordered");
            }

            int requestedQty = cartItem.getQuantity();
            if (product.getStockQuantity() < requestedQty) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product '" + product.getName() + "'. Available: " +
                        product.getStockQuantity() + ", Requested: " + requestedQty
                );
            }

            // Deduct stock from inventory
            product.setStockQuantity(product.getStockQuantity() - requestedQty);
            productRepository.save(product);

            // Authoritative server-side price calculation
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            subtotal = subtotal.add(lineTotal);

            // Snapshot product name and unit price at time of purchase
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductNameSnapshot(product.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(requestedQty);
            orderItem.setLineTotal(lineTotal);
            orderItems.add(orderItem);
        }

        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2);
        BigDecimal grandTotal = subtotal.add(shippingFee);

        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setGrandTotal(grandTotal);

        Order savedOrder = orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(savedOrder);
            orderItemRepository.save(oi);
            savedOrder.getItems().add(oi);
        }

        // Clear cart items only after order is successfully created
        cart.clearItems();
        cartRepository.save(cart);

        return mapToOrderDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        return orderRepository.findByUserOrderByPlacedAtDesc(user).stream()
                .map(this::mapToOrderDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }

        return mapToOrderDto(order);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public OrderResponseDto mapToOrderDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getOrderItemId(),
                        item.getProduct().getProductId(),
                        item.getProductNameSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponseDto(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getGrandTotal(),
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getPlacedAt(),
                itemDtos
        );
    }
}
