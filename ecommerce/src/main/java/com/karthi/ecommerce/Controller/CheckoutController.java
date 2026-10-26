package com.karthi.ecommerce.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.CheckoutRequestDto;
import com.karthi.ecommerce.DTO.OrderResponseDto;
import com.karthi.ecommerce.Service.CheckoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Executes checkout: validates cart and stock, creates order, snapshots prices,
     * decrements inventory, and clears cart transactionally.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        OrderResponseDto order = checkoutService.checkout(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Retrieve the order history for the authenticated user.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(checkoutService.getMyOrders(authentication.getName()));
    }

    /**
     * Retrieve details for a specific order owned by the authenticated user.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderDetails(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(checkoutService.getOrderDetails(authentication.getName(), orderId));
    }
}
