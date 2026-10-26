package com.shopstack.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopstack.Model.StockMovement;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventory_InventoryIdOrderByCreatedAtDesc(Long inventoryId);
    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
