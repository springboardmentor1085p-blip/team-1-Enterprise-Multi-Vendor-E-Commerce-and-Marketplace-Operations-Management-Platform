package com.shopstack.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopstack.DTO.PaymentRequestDto;
import com.shopstack.DTO.PaymentResponseDto;
import com.shopstack.Model.User;
import com.shopstack.Repository.UserRepository;
import com.shopstack.Service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDto> processPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(paymentService.processPayment(user.getUserId(), request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDto>> getOrderPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(paymentService.getOrderPayments(orderId, user.getUserId()));
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<PaymentResponseDto> verifyPayment(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }
}
