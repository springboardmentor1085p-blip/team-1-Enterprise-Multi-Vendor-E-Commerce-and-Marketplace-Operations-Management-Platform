package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.Enum.VendorStatus;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUser(User user);
    Optional<Vendor> findByUserUserId(Long userId);
    Optional<Vendor> findByBusinessName(String businessName);
    List<Vendor> findByStatus(VendorStatus status);
}
