package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.Enum.RefundStatus;
import com.karthi.ecommerce.Model.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPayment_Order_OrderId(Long orderId);
    List<Refund> findByPayment_PaymentId(Long paymentId);
    List<Refund> findByPayment_Order_User_UserId(Long userId);
    List<Refund> findByStatus(RefundStatus status);
    Optional<Refund> findByPayment_PaymentIdAndStatusNot(Long paymentId, RefundStatus status);
}
