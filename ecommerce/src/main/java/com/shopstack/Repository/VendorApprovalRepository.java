package com.shopstack.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopstack.Model.Vendor;
import com.shopstack.Model.VendorApproval;

@Repository
public interface VendorApprovalRepository extends JpaRepository<VendorApproval, Long> {
    List<VendorApproval> findByVendor(Vendor vendor);
}
