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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.RefundRequestDto;
import com.karthi.ecommerce.DTO.RefundResponseDto;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Service.RefundService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;
    private final UserRepository userRepository;

    public RefundController(RefundService refundService, UserRepository userRepository) {
        this.refundService = refundService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<RefundResponseDto> requestRefund(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RefundRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.requestRefund(user.getUserId(), request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<RefundResponseDto>> getOrderRefunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(refundService.getOrderRefunds(orderId, user.getUserId(), isAdmin));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<RefundResponseDto>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/process")
    public ResponseEntity<RefundResponseDto> processRefund(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("approve") boolean approve,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(refundService.processRefund(user.getUserId(), id, approve, rejectionReason));
    }
}
