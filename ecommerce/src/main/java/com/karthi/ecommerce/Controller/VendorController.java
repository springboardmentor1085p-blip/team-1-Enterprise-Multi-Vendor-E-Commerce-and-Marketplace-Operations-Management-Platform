package com.karthi.ecommerce.Controller;

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

import com.karthi.ecommerce.DTO.VendorResponseDto;
import com.karthi.ecommerce.Model.VendorApproval;
import com.karthi.ecommerce.Model.VendorStatus;
import com.karthi.ecommerce.Service.VendorService;

@RestController
@RequestMapping("/api")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // Public endpoint: Get approved vendors (no auth needed)
    @GetMapping("/vendors")
    public ResponseEntity<List<VendorResponseDto>> getApprovedVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors(VendorStatus.APPROVED));
    }

    // Public endpoint: Get vendor by ID (no auth needed)
    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<VendorResponseDto> getVendorById(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorService.getVendorById(vendorId));
    }

    // Vendor self-service: View own store profile
    // SecurityConfig: /api/vendor/** → ROLE_VENDOR or ROLE_ADMIN
    // @PreAuthorize: Extra method-level guard (defence in depth)
    @GetMapping("/vendor/profile")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorResponseDto> getMyVendorProfile(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorByEmail(authentication.getName()));
    }

    // Vendor self-service: Update own store profile
    @PutMapping("/vendor/profile")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<VendorResponseDto> updateMyVendorProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {
        String businessName = body.get("businessName");
        return ResponseEntity.ok(vendorService.updateVendorProfile(authentication.getName(), businessName));
    }

    // Admin: List all vendors with optional status filter
    // SecurityConfig: /api/admin/** → ROLE_ADMIN only
    // @PreAuthorize: Extra method-level guard (defence in depth)
    @GetMapping("/admin/vendors")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VendorResponseDto>> getAllVendorsAdmin(
            @RequestParam(required = false) VendorStatus status
    ) {
        return ResponseEntity.ok(vendorService.getAllVendors(status));
    }

    // Admin: Approve / Reject / Suspend vendor and record in vendor_approvals
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

    // Admin: View full approval history for a vendor
    @GetMapping("/admin/vendors/{vendorId}/approvals")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VendorApproval>> getVendorApprovalHistory(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorService.getVendorApprovalHistory(vendorId));
    }
}
