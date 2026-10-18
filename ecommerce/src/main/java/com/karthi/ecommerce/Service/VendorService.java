package com.karthi.ecommerce.Service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorApproval;
import com.karthi.ecommerce.Model.VendorStatus;
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

    @Transactional(readOnly = true)
    public List<Vendor> getAllVendors(VendorStatus status) {
        if (status != null) {
            return vendorRepository.findByStatus(status);
        }
        return vendorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Vendor getVendorById(Long vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with ID: " + vendorId));
    }

    @Transactional(readOnly = true)
    public Vendor getVendorByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return vendorRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No vendor profile associated with this account"));
    }

    @Transactional
    public Vendor updateVendorProfile(String email, String businessName) {
        Vendor vendor = getVendorByEmail(email);

        if (businessName != null && !businessName.isBlank()) {
            vendor.setBusinessName(businessName);
        }

        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor updateVendorStatus(Long vendorId, VendorStatus status, String adminEmail, String notes) {
        Vendor vendor = getVendorById(vendorId);
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
        approval.setReviewedAt(OffsetDateTime.now());
        vendorApprovalRepository.save(approval);

        return updatedVendor;
    }

    @Transactional(readOnly = true)
    public List<VendorApproval> getVendorApprovalHistory(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        return vendorApprovalRepository.findByVendor(vendor);
    }
}
