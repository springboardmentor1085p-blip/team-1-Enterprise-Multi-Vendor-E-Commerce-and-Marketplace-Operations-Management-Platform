package com.shopstack.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shopstack.Enum.ProductStatus;
import com.shopstack.Model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendor_VendorId(Long vendorId);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory_CategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.status = 'APPROVED' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("query") String query);

    /**
     * Paginated, multi-field filter query for public catalog browsing.
     * All filter parameters are optional (null means "no filter applied").
     */
    @Query(value = "SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.variants v " +
           "WHERE p.status = 'APPROVED' " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:brandId    IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:minPrice   IS NULL OR v.price >= :minPrice) " +
           "AND (:maxPrice   IS NULL OR v.price <= :maxPrice) " +
           "AND (:keyword    IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inStockOnly = false OR " +
           "     EXISTS (SELECT i FROM Inventory i WHERE i.variant = v " +
           "             AND (i.quantityOnHand - i.quantityReserved) > 0))",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Product p " +
           "LEFT JOIN p.variants v " +
           "WHERE p.status = 'APPROVED' " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:brandId    IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:minPrice   IS NULL OR v.price >= :minPrice) " +
           "AND (:maxPrice   IS NULL OR v.price <= :maxPrice) " +
           "AND (:keyword    IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inStockOnly = false OR " +
           "     EXISTS (SELECT i FROM Inventory i WHERE i.variant = v " +
           "             AND (i.quantityOnHand - i.quantityReserved) > 0))")
    Page<Product> findWithFilters(
            @Param("categoryId")  Long categoryId,
            @Param("brandId")     Long brandId,
            @Param("minPrice")    BigDecimal minPrice,
            @Param("maxPrice")    BigDecimal maxPrice,
            @Param("keyword")     String keyword,
            @Param("inStockOnly") boolean inStockOnly,
            Pageable pageable);

    /** Simple paginated list of all APPROVED products (no extra filters). */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
