package com.shopstack.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopstack.DTO.InventoryUpdateDto;
import com.shopstack.DTO.WarehouseDto;
import com.shopstack.Model.Inventory;
import com.shopstack.Model.User;
import com.shopstack.Model.Warehouse;
import com.shopstack.Repository.UserRepository;
import com.shopstack.Service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService, UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@Valid @RequestBody WarehouseDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createWarehouse(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(inventoryService.getAllActiveWarehouses());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    @PostMapping("/update")
    public ResponseEntity<Inventory> updateStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InventoryUpdateDto dto) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(inventoryService.updateStock(user.getUserId(), dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @GetMapping("/variant/{variantId}/stock")
    public ResponseEntity<Integer> getVariantStock(@PathVariable Long variantId) {
        return ResponseEntity.ok(inventoryService.getAvailableStock(variantId));
    }
}
