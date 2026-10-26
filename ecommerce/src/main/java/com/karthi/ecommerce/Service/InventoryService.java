package com.karthi.ecommerce.Service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.MovementType;
import com.karthi.ecommerce.DTO.InventoryUpdateDto;
import com.karthi.ecommerce.DTO.WarehouseDto;
import com.karthi.ecommerce.Model.Inventory;
import com.karthi.ecommerce.Model.ProductVariant;
import com.karthi.ecommerce.Model.StockMovement;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Warehouse;
import com.karthi.ecommerce.Repository.InventoryRepository;
import com.karthi.ecommerce.Repository.ProductVariantRepository;
import com.karthi.ecommerce.Repository.StockMovementRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.WarehouseRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository variantRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            WarehouseRepository warehouseRepository,
                            ProductVariantRepository variantRepository,
                            StockMovementRepository stockMovementRepository,
                            UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.variantRepository = variantRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
    }

    public Warehouse createWarehouse(WarehouseDto dto) {
        Warehouse w = new Warehouse();
        w.setName(dto.getName());
        w.setAddressLine(dto.getAddressLine());
        w.setCity(dto.getCity());
        w.setState(dto.getState());
        w.setPostalCode(dto.getPostalCode());
        w.setCountry(dto.getCountry());
        w.setActive(dto.isActive());
        return warehouseRepository.save(w);
    }

    public List<Warehouse> getAllActiveWarehouses() {
        return warehouseRepository.findByIsActiveTrue();
    }

    @Transactional
    public Inventory updateStock(Long userId, InventoryUpdateDto dto) {
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + dto.getWarehouseId()));

        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + dto.getVariantId()));

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Inventory inventory = inventoryRepository.findByWarehouse_WarehouseIdAndVariant_VariantId(
                dto.getWarehouseId(), dto.getVariantId()
        ).orElseGet(() -> {
            Inventory newInv = new Inventory();
            newInv.setWarehouse(warehouse);
            newInv.setVariant(variant);
            newInv.setQuantityOnHand(0);
            newInv.setQuantityReserved(0);
            newInv.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 10);
            return newInv;
        });

        int newQuantity = inventory.getQuantityOnHand() + dto.getQuantityDelta();
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Cannot reduce stock below 0. Current on hand: " + inventory.getQuantityOnHand());
        }

        inventory.setQuantityOnHand(newQuantity);
        if (dto.getReorderLevel() != null) {
            inventory.setReorderLevel(dto.getReorderLevel());
        }
        inventory.setUpdatedAt(OffsetDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);

        // Record stock movement
        StockMovement sm = new StockMovement();
        sm.setInventory(saved);
        sm.setChangedByUser(user);
        sm.setMovementType(dto.getQuantityDelta() >= 0 ? MovementType.INBOUND : MovementType.OUTBOUND);
        sm.setQuantityDelta(dto.getQuantityDelta());
        sm.setReferenceType(dto.getReason() != null ? dto.getReason() : "MANUAL_ADJUSTMENT");
        sm.setReferenceId(saved.getInventoryId());
        stockMovementRepository.save(sm);

        return saved;
    }

    @Transactional
    public void reserveStock(Long variantId, int quantity, Long orderId, User user) {
        // Use pessimistic lock to prevent overselling and concurrency races
        List<Inventory> inventories = inventoryRepository.findByVariant_VariantIdWithLock(variantId);
        int remainingToReserve = quantity;

        for (Inventory inv : inventories) {
            int availableInWarehouse = inv.getQuantityOnHand() - inv.getQuantityReserved();
            if (availableInWarehouse > 0) {
                int reserveFromThis = Math.min(availableInWarehouse, remainingToReserve);
                inv.setQuantityReserved(inv.getQuantityReserved() + reserveFromThis);
                inv.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inv);

                StockMovement sm = new StockMovement();
                sm.setInventory(inv);
                sm.setChangedByUser(user);
                sm.setMovementType(MovementType.RESERVATION);
                sm.setQuantityDelta(reserveFromThis);
                sm.setReferenceType("ORDER");
                sm.setReferenceId(orderId);
                stockMovementRepository.save(sm);

                remainingToReserve -= reserveFromThis;
                if (remainingToReserve == 0) break;
            }
        }

        if (remainingToReserve > 0) {
            throw new IllegalArgumentException("Insufficient inventory to reserve for variant ID: " + variantId);
        }
    }

    @Transactional
    public void releaseStock(Long variantId, int quantity, Long orderId, User user) {
        // Use pessimistic lock to prevent concurrent release errors
        List<Inventory> inventories = inventoryRepository.findByVariant_VariantIdWithLock(variantId);
        int remainingToRelease = quantity;

        for (Inventory inv : inventories) {
            if (inv.getQuantityReserved() > 0) {
                int releaseFromThis = Math.min(inv.getQuantityReserved(), remainingToRelease);
                inv.setQuantityReserved(inv.getQuantityReserved() - releaseFromThis);
                inv.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inv);

                StockMovement sm = new StockMovement();
                sm.setInventory(inv);
                sm.setChangedByUser(user);
                sm.setMovementType(MovementType.RELEASE);
                sm.setQuantityDelta(-releaseFromThis);
                sm.setReferenceType("ORDER_CANCEL");
                sm.setReferenceId(orderId);
                stockMovementRepository.save(sm);

                remainingToRelease -= releaseFromThis;
                if (remainingToRelease == 0) break;
            }
        }
    }

    public List<Inventory> getLowStockAlerts() {
        return inventoryRepository.findLowStockInventories();
    }

    public Integer getAvailableStock(Long variantId) {
        Long stock = inventoryRepository.getTotalAvailableQuantityForVariant(variantId);
        return stock != null ? stock.intValue() : 0;
    }
}
