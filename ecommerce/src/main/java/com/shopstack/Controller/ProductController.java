package com.shopstack.Controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopstack.Enum.ProductStatus;
import com.shopstack.DTO.PageResponse;
import com.shopstack.DTO.ProductRequestDto;
import com.shopstack.DTO.ProductResponseDto;
import com.shopstack.DTO.ProductStatusUpdateDto;
import com.shopstack.DTO.ProductUpdateRequestDto;
import com.shopstack.Model.User;
import com.shopstack.Repository.UserRepository;
import com.shopstack.Service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    // ─── Vendor Endpoints ────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    @PostMapping("/vendor/products")
    public ResponseEntity<ProductResponseDto> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ProductResponseDto product = productService.createProduct(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    @GetMapping("/vendor/products")
    public ResponseEntity<List<ProductResponseDto>> getVendorProducts(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(productService.getVendorProducts(user.getUserId()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    @PutMapping("/vendor/products/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequestDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ProductResponseDto updated = productService.updateProduct(user.getUserId(), productId, request, isAdmin);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    @DeleteMapping("/vendor/products/{productId}")
    public ResponseEntity<ProductResponseDto> deleteProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ProductResponseDto softDeleted = productService.deleteProduct(user.getUserId(), productId, isAdmin);
        return ResponseEntity.ok(softDeleted);
    }

    // ─── Admin Review & Approval Endpoints ───────────────────────────────────────

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/products/pending")
    public ResponseEntity<List<ProductResponseDto>> getPendingProducts() {
        return ResponseEntity.ok(productService.getPendingProducts());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/products")
    public ResponseEntity<List<ProductResponseDto>> getAllProductsAdmin(
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.getAdminProducts(status));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/admin/products/{productId}/status")
    public ResponseEntity<ProductResponseDto> updateProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateDto request) {
        ProductResponseDto updated = productService.updateProductStatus(
                productId, request.getStatus(), request.getNotes());
        return ResponseEntity.ok(updated);
    }

    // ─── Public Catalog Endpoints (Browsing, Filtering & Pagination) ─────────────

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponseDto>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean inStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return ResponseEntity.ok(productService.getProductsWithFilters(
                categoryId, brandId, minPrice, maxPrice, keyword, inStockOnly, pageable));
    }

    @GetMapping("/products/all")
    public ResponseEntity<List<ProductResponseDto>> getAllProductsUnpaginated() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponseDto>> searchProducts(@RequestParam("q") String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDto>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────────

    private Pageable createPageable(int page, int size, String sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100)); // Cap size at 100 to prevent DoS

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim().toLowerCase();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Whitelist allowed sort fields to prevent property injection
        String mappedProperty;
        switch (property) {
            case "price":
                mappedProperty = "variants.price";
                break;
            case "name":
                mappedProperty = "name";
                break;
            case "id":
            case "productid":
                mappedProperty = "productId";
                break;
            case "createdat":
            default:
                mappedProperty = "createdAt";
                break;
        }

        return PageRequest.of(safePage, safeSize, Sort.by(direction, mappedProperty));
    }
}

