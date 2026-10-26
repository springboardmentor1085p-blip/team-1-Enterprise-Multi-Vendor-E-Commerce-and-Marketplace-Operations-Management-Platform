package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.Inventory;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByWarehouse_WarehouseIdAndVariant_VariantId(Long warehouseId, Long variantId);
    List<Inventory> findByVariant_VariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.variantId = :variantId")
    List<Inventory> findByVariant_VariantIdWithLock(@Param("variantId") Long variantId);

    @Query("SELECT i FROM Inventory i WHERE (i.quantityOnHand - i.quantityReserved) <= i.reorderLevel")
    List<Inventory> findLowStockInventories();

    @Query("SELECT COALESCE(SUM(i.quantityOnHand - i.quantityReserved), 0L) FROM Inventory i WHERE i.variant.variantId = :variantId")
    Long getTotalAvailableQuantityForVariant(@Param("variantId") Long variantId);
}
