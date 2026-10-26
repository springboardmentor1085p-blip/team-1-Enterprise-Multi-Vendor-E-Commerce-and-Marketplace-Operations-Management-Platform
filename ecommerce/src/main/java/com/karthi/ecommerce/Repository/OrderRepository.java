package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByPlacedAtDesc(User user);
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByOrderIdAndUser(Long orderId, User user);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}
