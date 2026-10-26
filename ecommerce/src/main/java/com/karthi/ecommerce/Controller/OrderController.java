package com.karthi.ecommerce.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.CheckoutRequestDto;
import com.karthi.ecommerce.DTO.OrderCancelRequestDto;
import com.karthi.ecommerce.DTO.OrderResponseDto;
import com.karthi.ecommerce.DTO.OrderStatusUpdateDto;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(user.getUserId(), request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(orderService.getUserOrders(user.getUserId()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(orderService.getOrderById(id, user.getUserId()));
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<OrderResponseDto> trackOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody OrderCancelRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.cancelOrder(id, user.getUserId(), isAdmin, request.getReason()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(orderService.updateOrderStatus(id, user.getUserId(), request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/expire-unpaid")
    public ResponseEntity<String> triggerExpiration() {
        orderService.expireUnpaidOrders();
        return ResponseEntity.ok("Unpaid order expiration process triggered successfully");
    }
}
