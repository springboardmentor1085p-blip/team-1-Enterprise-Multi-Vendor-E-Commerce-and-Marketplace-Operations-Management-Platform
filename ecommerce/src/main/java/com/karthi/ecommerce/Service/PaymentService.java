package com.karthi.ecommerce.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.OrderStatus;
import com.karthi.Enum.PaymentStatus;
import com.karthi.ecommerce.DTO.PaymentRequestDto;
import com.karthi.ecommerce.DTO.PaymentResponseDto;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.Payment;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          @Lazy OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponseDto processPayment(Long userId, PaymentRequestDto dto) {
        // 1. Idempotency Check
        if (dto.getIdempotencyKey() != null && !dto.getIdempotencyKey().isBlank()) {
            var existingWithKey = paymentRepository.findByIdempotencyKey(dto.getIdempotencyKey());
            if (existingWithKey.isPresent()) {
                return mapToDto(existingWithKey.get());
            }
        }

        // 2. Load and validate Order
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderId()));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // 3. Prevent duplicate payment if already paid
        var completedPayment = paymentRepository.findFirstByOrder_OrderIdAndStatus(order.getOrderId(), PaymentStatus.COMPLETED);
        if (completedPayment.isPresent() || order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order " + order.getOrderNumber() + " has already been paid successfully. Duplicate payment rejected.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay for cancelled order: " + order.getOrderNumber());
        }

        // 4. Create or reuse pending payment record
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
        payment.setIdempotencyKey(dto.getIdempotencyKey());

        // 5. Sandbox outcome simulation
        String outcome = dto.getSandboxOutcome() != null ? dto.getSandboxOutcome().toUpperCase().trim() : "SUCCESS";
        payment.setSandboxOutcome(outcome);

        switch (outcome) {
            case "SUCCESS":
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setFailureReason(null);
                payment.setPaidAt(OffsetDateTime.now());
                Payment savedSuccess = paymentRepository.save(payment);
                // Confirm the order
                orderService.confirmPaidOrder(order.getOrderId(), savedSuccess);
                return mapToDto(savedSuccess);

            case "DECLINED":
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Transaction declined: Card declined by issuing bank");
                payment.setPaidAt(null);
                return mapToDto(paymentRepository.save(payment));

            case "INSUFFICIENT_FUNDS":
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Transaction declined: Insufficient funds in account");
                payment.setPaidAt(null);
                return mapToDto(paymentRepository.save(payment));

            case "TIMEOUT":
                payment.setStatus(PaymentStatus.PENDING);
                payment.setFailureReason("Transaction pending: Gateway timed out. Awaiting reconciliation.");
                payment.setPaidAt(null);
                return mapToDto(paymentRepository.save(payment));

            default:
                throw new IllegalArgumentException("Unsupported sandbox outcome: " + outcome + ". Supported: SUCCESS, DECLINED, TIMEOUT, INSUFFICIENT_FUNDS");
        }
    }

    public List<PaymentResponseDto> getOrderPayments(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return paymentRepository.findByOrder_OrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PaymentResponseDto getPaymentByReference(String reference) {
        Payment payment = paymentRepository.findByProviderReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for reference: " + reference));
        return mapToDto(payment);
    }

    public PaymentResponseDto mapToDto(Payment p) {
        return PaymentResponseDto.builder()
                .paymentId(p.getPaymentId())
                .orderId(p.getOrder().getOrderId())
                .orderNumber(p.getOrder().getOrderNumber())
                .provider(p.getProvider())
                .providerReference(p.getProviderReference())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .sandboxOutcome(p.getSandboxOutcome())
                .idempotencyKey(p.getIdempotencyKey())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
