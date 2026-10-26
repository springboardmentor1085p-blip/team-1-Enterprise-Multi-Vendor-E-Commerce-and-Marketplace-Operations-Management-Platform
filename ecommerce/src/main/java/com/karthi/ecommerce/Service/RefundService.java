package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.OrderStatus;
import com.karthi.Enum.PaymentStatus;
import com.karthi.Enum.RefundStatus;
import com.karthi.ecommerce.DTO.OrderStatusUpdateDto;
import com.karthi.ecommerce.DTO.RefundRequestDto;
import com.karthi.ecommerce.DTO.RefundResponseDto;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.OrderItem;
import com.karthi.ecommerce.Model.Payment;
import com.karthi.ecommerce.Model.Refund;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.PaymentRepository;
import com.karthi.ecommerce.Repository.RefundRepository;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public RefundResponseDto requestRefund(Long userId, RefundRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderId()));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        Payment payment = paymentRepository.findFirstByOrder_OrderIdAndStatus(order.getOrderId(), PaymentStatus.COMPLETED)
                .orElseThrow(() -> new IllegalStateException("Cannot refund an order without a completed payment"));

        // Check if there is already an active refund
        var existing = refundRepository.findByPayment_PaymentIdAndStatusNot(payment.getPaymentId(), RefundStatus.REJECTED);
        if (existing.isPresent()) {
            throw new IllegalStateException("A refund is already requested or processed for this payment");
        }

        BigDecimal refundAmount = dto.getAmount() != null ? dto.getAmount() : payment.getAmount();
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount: " + payment.getAmount());
        }

        OrderItem item = null;
        if (dto.getOrderItemId() != null) {
            item = orderItemRepository.findById(dto.getOrderItemId()).orElse(null);
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setOrderItem(item);
        refund.setAmount(refundAmount);
        refund.setReason(dto.getReason());
        refund.setStatus(RefundStatus.PENDING);

        Refund saved = refundRepository.save(refund);
        return mapToDto(saved);
    }

    @Transactional
    public Refund initiateAutomaticRefund(Order order, Payment payment, String reason) {
        // Prevent duplicate automatic refund
        var existing = refundRepository.findByPayment_PaymentIdAndStatusNot(payment.getPaymentId(), RefundStatus.REJECTED);
        if (existing.isPresent()) {
            return existing.get();
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProviderRefundReference("REF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        refund.setProcessedAt(OffsetDateTime.now());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return refundRepository.save(refund);
    }

    @Transactional
    public RefundResponseDto processRefund(Long adminUserId, Long refundId, boolean approve, String rejectionReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Refund is already " + refund.getStatus());
        }

        if (approve) {
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProviderRefundReference("REF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            refund.setProcessedAt(OffsetDateTime.now());

            Payment payment = refund.getPayment();
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
        } else {
            refund.setStatus(RefundStatus.REJECTED);
            if (rejectionReason != null && !rejectionReason.isBlank()) {
                refund.setReason(refund.getReason() + " | Rejection: " + rejectionReason);
            }
        }

        return mapToDto(refundRepository.save(refund));
    }

    public List<RefundResponseDto> getOrderRefunds(Long orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!isAdmin && !order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return refundRepository.findByPayment_Order_OrderId(orderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RefundResponseDto> getAllRefunds() {
        return refundRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RefundResponseDto mapToDto(Refund r) {
        return RefundResponseDto.builder()
                .refundId(r.getRefundId())
                .paymentId(r.getPayment().getPaymentId())
                .orderId(r.getPayment().getOrder().getOrderId())
                .orderNumber(r.getPayment().getOrder().getOrderNumber())
                .orderItemId(r.getOrderItem() != null ? r.getOrderItem().getOrderItemId() : null)
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .providerRefundReference(r.getProviderRefundReference())
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }
}
