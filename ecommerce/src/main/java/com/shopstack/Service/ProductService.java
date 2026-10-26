package com.shopstack.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopstack.Enum.MovementType;
import com.shopstack.Enum.ProductStatus;
import com.shopstack.Enum.VendorStatus;
import com.shopstack.DTO.PageResponse;
import com.shopstack.DTO.ProductRequestDto;
import com.shopstack.DTO.ProductResponseDto;
import com.shopstack.DTO.ProductUpdateRequestDto;
import com.shopstack.Model.Brand;
import com.shopstack.Model.Category;
import com.shopstack.Model.Inventory;
import com.shopstack.Model.Product;
import com.shopstack.Model.ProductImage;
import com.shopstack.Model.ProductVariant;
import com.shopstack.Model.StockMovement;
import com.shopstack.Model.Vendor;
import com.shopstack.Model.Warehouse;
import com.shopstack.Repository.BrandRepository;
import com.shopstack.Repository.CategoryRepository;
import com.shopstack.Repository.InventoryRepository;
import com.shopstack.Repository.ProductImageRepository;
import com.shopstack.Repository.ProductRepository;
import com.shopstack.Repository.ProductVariantRepository;
import com.shopstack.Repository.StockMovementRepository;
import com.shopstack.Repository.VendorRepository;
import com.shopstack.Repository.WarehouseRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final VendorRepository vendorRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository variantRepository,
                          ProductImageRepository imageRepository,
                          CategoryRepository categoryRepository,
                          BrandRepository brandRepository,
                          VendorRepository vendorRepository,
                          WarehouseRepository warehouseRepository,
                          InventoryRepository inventoryRepository,
                          StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.vendorRepository = vendorRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // ─── Create Product (Milestone 1 + Milestone 2: PENDING_APPROVAL) ─────────────

    @Transactional
    public ProductResponseDto createProduct(Long vendorUserId, ProductRequestDto dto) {
        Vendor vendor = vendorRepository.findByUserUserId(vendorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found for user: " + vendorUserId));

        // Security check: Only approved vendors can create products
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new AccessDeniedException("Vendor store status is " + vendor.getStatus() + ". Only approved vendors can create products.");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));

        Brand brand = null;
        if (dto.getBrandId() != null) {
            brand = brandRepository.findById(dto.getBrandId()).orElse(null);
        }

        Product product = new Product();
        product.setVendor(vendor);
        product.setCategory(category);
        product.setBrand(brand);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        // Milestone 2 Requirement: New products start in PENDING_APPROVAL status
        product.setStatus(ProductStatus.PENDING_APPROVAL);

        Product savedProduct = productRepository.save(product);

        // Images
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            int sortOrder = 1;
            for (String url : dto.getImageUrls()) {
                ProductImage img = new ProductImage();
                img.setProduct(savedProduct);
                img.setImageUrl(url);
                img.setSortOrder(sortOrder++);
                imageRepository.save(img);
            }
        }

        // Variants & Stock
        if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
            for (ProductRequestDto.VariantRequestDto varDto : dto.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(savedProduct);
                variant.setSku(varDto.getSku());
                variant.setVariantName(varDto.getVariantName());
                variant.setAttributesJson(varDto.getAttributesJson());
                variant.setPrice(varDto.getPrice());
                variant.setActive(true);
                ProductVariant savedVariant = variantRepository.save(variant);

                if (varDto.getInitialStock() != null && varDto.getInitialStock() > 0) {
                    Warehouse warehouse = null;
                    if (varDto.getWarehouseId() != null) {
                        warehouse = warehouseRepository.findById(varDto.getWarehouseId()).orElse(null);
                    }
                    if (warehouse == null) {
                        List<Warehouse> activeWarehouses = warehouseRepository.findByIsActiveTrue();
                        if (activeWarehouses.isEmpty()) {
                            Warehouse defaultWh = new Warehouse();
                            defaultWh.setName("Main Central Warehouse");
                            defaultWh.setActive(true);
                            warehouse = warehouseRepository.save(defaultWh);
                        } else {
                            warehouse = activeWarehouses.get(0);
                        }
                    }

                    Inventory inventory = new Inventory();
                    inventory.setWarehouse(warehouse);
                    inventory.setVariant(savedVariant);
                    inventory.setQuantityOnHand(varDto.getInitialStock());
                    inventory.setQuantityReserved(0);
                    inventory.setReorderLevel(10);
                    inventory.setUpdatedAt(OffsetDateTime.now());
                    Inventory savedInv = inventoryRepository.save(inventory);

                    StockMovement sm = new StockMovement();
                    sm.setInventory(savedInv);
                    sm.setMovementType(MovementType.INBOUND);
                    sm.setQuantityDelta(varDto.getInitialStock());
                    sm.setReferenceType("INITIAL_STOCK");
                    sm.setReferenceId(savedVariant.getVariantId());
                    sm.setChangedByUser(vendor.getUser());
                    stockMovementRepository.save(sm);
                }
            }
        }

        return getProductById(savedProduct.getProductId());
    }

    // ─── Milestone 2 Task 2.1: Product Update & Soft Deletion ────────────────────

    @Transactional
    public ProductResponseDto updateProduct(Long vendorUserId, Long productId, ProductUpdateRequestDto dto, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Security check: Vendor A cannot update a product belonging to Vendor B
        if (!isAdmin) {
            Vendor vendor = vendorRepository.findByUserUserId(vendorUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found for user: " + vendorUserId));
            if (vendor.getStatus() != VendorStatus.APPROVED) {
                throw new AccessDeniedException("Vendor store status is " + vendor.getStatus() + ". Only approved vendors can update products.");
            }
            if (!product.getVendor().getVendorId().equals(vendor.getVendorId())) {
                throw new AccessDeniedException("You do not have permission to update this product");
            }
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            product.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));
            product.setCategory(category);
        }
        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId()).orElse(null);
            product.setBrand(brand);
        }

        // When a vendor updates a product, if it was rejected or approved, reset to PENDING_APPROVAL
        if (!isAdmin && product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            product.setStatus(ProductStatus.PENDING_APPROVAL);
            product.setAdminNotes(null); // Clear previous rejection/approval notes
        }

        Product savedProduct = productRepository.save(product);

        // Update images if provided
        if (dto.getImageUrls() != null) {
            imageRepository.deleteByProduct_ProductId(savedProduct.getProductId());
            int sortOrder = 1;
            for (String url : dto.getImageUrls()) {
                ProductImage img = new ProductImage();
                img.setProduct(savedProduct);
                img.setImageUrl(url);
                img.setSortOrder(sortOrder++);
                imageRepository.save(img);
            }
        }

        // Update / Add variants if provided
        if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
            for (ProductUpdateRequestDto.VariantUpdateRequestDto varDto : dto.getVariants()) {
                ProductVariant variant;
                if (varDto.getVariantId() != null) {
                    variant = variantRepository.findById(varDto.getVariantId())
                            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + varDto.getVariantId()));
                    // IDOR Prevention: Ensure variant belongs to this product!
                    if (!variant.getProduct().getProductId().equals(savedProduct.getProductId())) {
                        throw new AccessDeniedException("Variant " + varDto.getVariantId() + " does not belong to this product");
                    }
                } else {
                    variant = new ProductVariant();
                    variant.setProduct(savedProduct);
                }

                if (varDto.getSku() != null && !varDto.getSku().isBlank()) {
                    variant.setSku(varDto.getSku());
                }
                if (varDto.getVariantName() != null) {
                    variant.setVariantName(varDto.getVariantName());
                }
                if (varDto.getAttributesJson() != null) {
                    variant.setAttributesJson(varDto.getAttributesJson());
                }
                if (varDto.getPrice() != null) {
                    variant.setPrice(varDto.getPrice());
                }
                if (varDto.getIsActive() != null) {
                    variant.setActive(varDto.getIsActive());
                }

                variantRepository.save(variant);
            }
        }

        return getProductById(savedProduct.getProductId());
    }

    @Transactional
    public ProductResponseDto deleteProduct(Long vendorUserId, Long productId, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Security check: Vendor A cannot delete Vendor B's product
        if (!isAdmin) {
            Vendor vendor = vendorRepository.findByUserUserId(vendorUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found for user: " + vendorUserId));
            if (!product.getVendor().getVendorId().equals(vendor.getVendorId())) {
                throw new AccessDeniedException("You do not have permission to delete this product");
            }
        }

        // Soft deletion: set status = INACTIVE rather than hard deletion
        product.setStatus(ProductStatus.INACTIVE);
        Product savedProduct = productRepository.save(product);
        return mapToDto(savedProduct);
    }

    // ─── Milestone 2 Task 2.2: Product Approval Workflow (Admin Review) ──────────

    @Transactional
    public ProductResponseDto updateProductStatus(Long productId, ProductStatus newStatus, String notes) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        product.setStatus(newStatus);
        if (notes != null) {
            product.setAdminNotes(notes);
        }
        Product savedProduct = productRepository.save(product);
        return mapToDto(savedProduct);
    }

    public List<ProductResponseDto> getPendingProducts() {
        return productRepository.findByStatus(ProductStatus.PENDING_APPROVAL).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getAdminProducts(ProductStatus status) {
        List<Product> products = (status != null)
                ? productRepository.findByStatus(status)
                : productRepository.findAll();
        return products.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─── Milestone 2 Task 2.3: Pagination & Multi-Field Filtering ────────────────

    public PageResponse<ProductResponseDto> getProductsWithFilters(
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword,
            Boolean inStockOnly,
            Pageable pageable) {

        boolean hasFilter = (categoryId != null) || (brandId != null) || (minPrice != null)
                || (maxPrice != null) || (keyword != null && !keyword.isBlank())
                || (inStockOnly != null && inStockOnly);

        Page<Product> page;
        if (hasFilter) {
            String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
            boolean stockFilter = (inStockOnly != null && inStockOnly);
            page = productRepository.findWithFilters(categoryId, brandId, minPrice, maxPrice, cleanKeyword, stockFilter, pageable);
        } else {
            page = productRepository.findByStatus(ProductStatus.APPROVED, pageable);
        }

        Page<ProductResponseDto> dtoPage = page.map(this::mapToDto);
        return PageResponse.from(dtoPage);
    }

    // ─── Query Endpoints ─────────────────────────────────────────────────────────

    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return mapToDto(product);
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findByStatus(ProductStatus.APPROVED).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategory_CategoryId(categoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getVendorProducts(Long vendorUserId) {
        Vendor vendor = vendorRepository.findByUserUserId(vendorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found"));
        return productRepository.findByVendor_VendorId(vendor.getVendorId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto mapToDto(Product p) {
        List<ProductVariant> variants = variantRepository.findByProduct_ProductId(p.getProductId());
        List<ProductImage> images = imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(p.getProductId());

        List<ProductResponseDto.VariantResponseDto> variantDtos = variants.stream().map(v -> {
            Integer availableStock = inventoryRepository.getTotalAvailableQuantityForVariant(v.getVariantId());
            return ProductResponseDto.VariantResponseDto.builder()
                    .variantId(v.getVariantId())
                    .sku(v.getSku())
                    .variantName(v.getVariantName())
                    .attributesJson(v.getAttributesJson())
                    .price(v.getPrice())
                    .isActive(v.isActive())
                    .availableStock(availableStock != null ? availableStock : 0)
                    .build();
        }).collect(Collectors.toList());

        List<String> imageUrls = images.stream().map(ProductImage::getImageUrl).collect(Collectors.toList());

        return ProductResponseDto.builder()
                .productId(p.getProductId())
                .vendorId(p.getVendor().getVendorId())
                .vendorBusinessName(p.getVendor().getBusinessName())
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandId(p.getBrand() != null ? p.getBrand().getBrandId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .name(p.getName())
                .description(p.getDescription())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .adminNotes(p.getAdminNotes())
                .variants(variantDtos)
                .imageUrls(imageUrls)
                .createdAt(p.getCreatedAt())
                .build();
    }
}

