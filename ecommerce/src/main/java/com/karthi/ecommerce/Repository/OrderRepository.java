package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.Enum.OrderStatus;
import com.karthi.ecommerce.Model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUser_UserIdOrderByPlacedAtDesc(Long userId);
    List<Order> findByStatus(OrderStatus status);
}
