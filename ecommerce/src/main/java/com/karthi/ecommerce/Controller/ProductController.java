package com.karthi.ecommerce.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.ProductRequestDto;
import com.karthi.ecommerce.DTO.ProductResponseDto;
import com.karthi.ecommerce.Service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Public endpoint: List all active products.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    /**
     * Public endpoint: Get product details by ID.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    /**
     * Vendor/Admin endpoint: Create a new product.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(authentication.getName(), request));
    }
}
