package com.shopstack.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopstack.DTO.VendorAnalyticsOverviewDto;
import com.shopstack.DTO.VendorCommissionResponseDto;
import com.shopstack.DTO.VendorCommissionUpdateDto;
import com.shopstack.DTO.VendorLowStockAlertDto;
import com.shopstack.DTO.VendorResponseDto;
import com.shopstack.DTO.VendorSaleItemDto;
import com.shopstack.DTO.VendorTopProductDto;
import com.shopstack.Enum.VendorStatus;
import com.shopstack.Model.VendorApproval;
import com.shopstack.Service.VendorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // ─── Public Endpoints (No auth needed) ───────────────────────────────────────

    @GetMapping("/vendors")
    public ResponseEntity<List<VendorResponseDto>> getApprovedVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors(VendorStatus.APPROVED));
    }

    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<VendorResponseDto> getVendorById(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorService.getVendorById(vendorId));
    }

    // ─── Vendor Self-Service Store Profile ───────────────────────────────────────

    @GetMapping("/vendor/profile")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorResponseDto> getMyVendorProfile(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorByEmail(authentication.getName()));
    }

    @PutMapping("/vendor/profile")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorResponseDto> updateMyVendorProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {
        String businessName = body.get("businessName");
        return ResponseEntity.ok(vendorService.updateVendorProfile(authentication.getName(), businessName));
    }

    // ─── Milestone 3: Vendor Commissions & Earnings ──────────────────────────────

    @GetMapping("/vendor/commissions")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorCommissionResponseDto> getMyCommissions(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorCommissionStatementByEmail(authentication.getName()));
    }

    // ─── Milestone 3: Vendor Dashboard & Analytics ───────────────────────────────

    @GetMapping("/vendor/analytics/overview")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorAnalyticsOverviewDto> getVendorAnalyticsOverview(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorAnalyticsOverview(authentication.getName()));
    }

    @GetMapping("/vendor/analytics/top-products")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<List<VendorTopProductDto>> getVendorTopProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(vendorService.getVendorTopProducts(authentication.getName(), limit));
    }

    @GetMapping("/vendor/analytics/low-stock")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<List<VendorLowStockAlertDto>> getVendorLowStockAlerts(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorLowStockAlerts(authentication.getName()));
    }

    // ─── Milestone 3: Vendor Order Fulfillment ──────────────────────────────────

    @GetMapping("/vendor/orders")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<List<VendorSaleItemDto>> getVendorOrderItems(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorOrderItems(authentication.getName()));
    }

    // ─── Admin Vendor Management & Governance ───────────────────────────────────

    @GetMapping("/admin/vendors")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VendorResponseDto>> getAllVendorsAdmin(
            @RequestParam(required = false) VendorStatus status
    ) {
        return ResponseEntity.ok(vendorService.getAllVendors(status));
    }

    @PatchMapping("/admin/vendors/{vendorId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VendorResponseDto> updateVendorStatus(
            Authentication authentication,
            @PathVariable Long vendorId,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        String notes = body.get("notes");
        VendorStatus status = VendorStatus.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(vendorService.updateVendorStatus(vendorId, status, authentication.getName(), notes));
    }

    @GetMapping("/admin/vendors/{vendorId}/approvals")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VendorApproval>> getVendorApprovalHistory(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorService.getVendorApprovalHistory(vendorId));
    }

    @PatchMapping("/admin/vendors/{vendorId}/commission")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VendorResponseDto> updateVendorCommission(
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorCommissionUpdateDto body
    ) {
        return ResponseEntity.ok(vendorService.updateCommissionRate(vendorId, body.getCommissionRate()));
    }

    @GetMapping("/admin/vendors/{vendorId}/commissions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VendorCommissionResponseDto> getVendorCommissionAdmin(
            @PathVariable Long vendorId
    ) {
        return ResponseEntity.ok(vendorService.getVendorCommissionStatement(vendorId));
    }

    @GetMapping("/admin/vendors/commissions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VendorCommissionResponseDto>> getAllVendorsCommissionAdmin() {
        return ResponseEntity.ok(vendorService.getAllVendorsCommissionSummary());
    }
}
