package com.karthi.ecommerce.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.OrderStatus;
import com.karthi.Enum.PaymentStatus;
import com.karthi.ecommerce.DTO.OrderStatusUpdateDto;
import com.karthi.ecommerce.DTO.PaymentRequestDto;
import com.karthi.ecommerce.DTO.PaymentResponseDto;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.Payment;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.PaymentRepository;


import com.karthi.ecommerce.Model.Notification;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

private final NotificationService notificationService;




public PaymentService(PaymentRepository paymentRepository,
                      OrderRepository orderRepository,
                      OrderService orderService,
                      NotificationService notificationService) {
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.orderService = orderService;
    this.notificationService = notificationService;
}


    @Transactional
    public PaymentResponseDto processPayment(Long userId, PaymentRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderId()));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // Check if there is already a payment or create new
        List<Payment> existingPayments = paymentRepository.findByOrder_OrderId(order.getOrderId());
        Payment payment = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setOrder(order);
                    return p;
                });

        payment.setProvider(dto.getProvider());
        payment.setProviderReference(dto.getProviderReference() != null ?
                dto.getProviderReference() : "PAY-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        payment.setAmount(dto.getAmount());
        payment.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "INR");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(OffsetDateTime.now());

        Payment saved = paymentRepository.save(payment);

        // Update order status to CONFIRMED
        OrderStatusUpdateDto statusDto = new OrderStatusUpdateDto();
        statusDto.setStatus(OrderStatus.CONFIRMED);
        statusDto.setComments("Payment received via " + dto.getProvider() + " (Ref: " + saved.getProviderReference() + ")");
        orderService.updateOrderStatus(order.getOrderId(), userId, statusDto);




notificationService.createNotification(
        order.getUser().getUserId(),
        "Payment successful",
        "Payment for order " + order.getOrderNumber()
                + " was completed successfully. Reference: "
                + saved.getProviderReference(),
        Notification.NotificationType.PAYMENT
);

        return mapToDto(saved);
    }

    public List<PaymentResponseDto> getOrderPayments(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return paymentRepository.findByOrder_OrderId(orderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PaymentResponseDto getPaymentByReference(String reference) {
        Payment payment = paymentRepository.findByProviderReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for reference: " + reference));
        return mapToDto(payment);
    }

    private PaymentResponseDto mapToDto(Payment p) {
        return PaymentResponseDto.builder()
                .paymentId(p.getPaymentId())
                .orderId(p.getOrder().getOrderId())
                .orderNumber(p.getOrder().getOrderNumber())
                .provider(p.getProvider())
                .providerReference(p.getProviderReference())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
