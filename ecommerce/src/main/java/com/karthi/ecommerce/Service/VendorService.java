package com.karthi.ecommerce.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.VendorStatus;
import com.karthi.ecommerce.DTO.VendorResponseDto;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorApproval;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorApprovalRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorApprovalRepository vendorApprovalRepository;
    private final UserRepository userRepository;

    public VendorService(
            VendorRepository vendorRepository,
            VendorApprovalRepository vendorApprovalRepository,
            UserRepository userRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorApprovalRepository = vendorApprovalRepository;
        this.userRepository = userRepository;
    }

    // ─── Internal helper: fetch raw Vendor entity (used within service layer only) ───

    @Transactional(readOnly = true)
    public Vendor getVendorEntityById(Long vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with ID: " + vendorId));
    }

    @Transactional(readOnly = true)
    public Vendor getVendorEntityByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return vendorRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No vendor profile associated with this account"));
    }

    // ─── Public API methods — return safe DTOs, never raw entities ───

    @Transactional(readOnly = true)
    public List<VendorResponseDto> getAllVendors(VendorStatus status) {
        List<Vendor> vendors = (status != null)
                ? vendorRepository.findByStatus(status)
                : vendorRepository.findAll();
        return vendors.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VendorResponseDto getVendorById(Long vendorId) {
        return toDto(getVendorEntityById(vendorId));
    }

    @Transactional(readOnly = true)
    public VendorResponseDto getVendorByEmail(String email) {
        return toDto(getVendorEntityByEmail(email));
    }

    @Transactional
    public VendorResponseDto updateVendorProfile(String email, String businessName) {
        Vendor vendor = getVendorEntityByEmail(email);
        if (businessName != null && !businessName.isBlank()) {
            vendor.setBusinessName(businessName);
        }
        return toDto(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponseDto updateVendorStatus(Long vendorId, VendorStatus status, String adminEmail, String notes) {
        Vendor vendor = getVendorEntityById(vendorId);
        vendor.setStatus(status);
        Vendor updatedVendor = vendorRepository.save(vendor);

        // Record approval / status decision in vendor_approvals table
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        VendorApproval approval = new VendorApproval();
        approval.setVendor(updatedVendor);
        approval.setReviewedByUser(admin);
        approval.setDecision(status.name());
        approval.setNotes(notes != null ? notes : "Status changed to " + status.name());
        vendorApprovalRepository.save(approval);

        return toDto(updatedVendor);
    }

    @Transactional(readOnly = true)
    public List<VendorApproval> getVendorApprovalHistory(Long vendorId) {
        Vendor vendor = getVendorEntityById(vendorId);
        return vendorApprovalRepository.findByVendor(vendor);
    }

    // ─── Mapper: Vendor entity → safe VendorResponseDto ───

    private VendorResponseDto toDto(Vendor vendor) {
        User user = vendor.getUser();
        return new VendorResponseDto(
                vendor.getVendorId(),
                vendor.getBusinessName(),
                vendor.getStatus().name(),
                vendor.getCommissionRate() != null ? vendor.getCommissionRate().toPlainString() : null,
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                vendor.getCreatedAt()
        );
    }
}
