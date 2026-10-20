package com.karthi.ecommerce.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karthi.Enum.ProductStatus;
import com.karthi.ecommerce.Model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendor_VendorId(Long vendorId);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory_CategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.status = 'APPROVED' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("query") String query);
}
