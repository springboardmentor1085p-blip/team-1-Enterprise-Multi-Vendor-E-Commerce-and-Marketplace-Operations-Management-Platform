package com.karthi.ecommerce.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.ProductRequestDto;
import com.karthi.ecommerce.DTO.ProductResponseDto;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorStatus;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long productId) {
        return toDto(getProductEntity(productId));
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
    }

    @Transactional
    public ProductResponseDto createProduct(String userEmail, ProductRequestDto request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName()));

        Vendor vendor = vendorRepository.findByUser(user).orElse(null);

        if (vendor == null) {
            if (isAdmin) {
                vendor = vendorRepository.findByStatus(VendorStatus.APPROVED).stream().findFirst()
                        .orElseGet(() -> vendorRepository.findAll().stream().findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("No vendor exists in the system to associate with this product")));
            } else {
                throw new IllegalArgumentException("No vendor profile associated with this account. Only registered vendors can create products.");
            }
        }

        if (!isAdmin && vendor.getStatus() != VendorStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Vendor account is not approved. Current status: " + vendor.getStatus() +
                    ". Only approved vendors can create products."
            );
        }

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(true);
        product.setVendor(vendor);

        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    public ProductResponseDto toDto(Product product) {
        return new ProductResponseDto(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive()
        );
    }
}
