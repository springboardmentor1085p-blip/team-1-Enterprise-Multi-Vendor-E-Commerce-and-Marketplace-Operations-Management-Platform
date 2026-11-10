package com.karthi.ecommerce.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.StockMovement;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventory_InventoryIdOrderByCreatedAtDesc(Long inventoryId);
    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
