package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.Enum.PaymentStatus;
import com.karthi.ecommerce.Model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder_OrderId(Long orderId);
    List<Payment> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Payment> findByProviderReference(String providerReference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findFirstByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);
}
