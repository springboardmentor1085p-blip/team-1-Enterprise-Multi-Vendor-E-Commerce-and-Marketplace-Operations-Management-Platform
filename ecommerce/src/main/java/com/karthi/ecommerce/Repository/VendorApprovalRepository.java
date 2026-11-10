package com.karthi.ecommerce.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorApproval;

@Repository
public interface VendorApprovalRepository extends JpaRepository<VendorApproval, Long> {
    List<VendorApproval> findByVendor(Vendor vendor);
}
