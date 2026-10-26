package com.karthi.ecommerce.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.MovementType;
import com.karthi.Enum.ProductStatus;
import com.karthi.ecommerce.DTO.ProductRequestDto;
import com.karthi.ecommerce.DTO.ProductResponseDto;
import com.karthi.ecommerce.Model.Brand;
import com.karthi.ecommerce.Model.Category;
import com.karthi.ecommerce.Model.Inventory;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.ProductImage;
import com.karthi.ecommerce.Model.ProductVariant;
import com.karthi.ecommerce.Model.StockMovement;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.Warehouse;
import com.karthi.ecommerce.Repository.BrandRepository;
import com.karthi.ecommerce.Repository.CategoryRepository;
import com.karthi.ecommerce.Repository.InventoryRepository;
import com.karthi.ecommerce.Repository.ProductImageRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.ProductVariantRepository;
import com.karthi.ecommerce.Repository.StockMovementRepository;
import com.karthi.ecommerce.Repository.VendorRepository;
import com.karthi.ecommerce.Repository.WarehouseRepository;

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

    @Transactional
    public ProductResponseDto createProduct(Long vendorUserId, ProductRequestDto dto) {
        Vendor vendor = vendorRepository.findByUserUserId(vendorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found for user: " + vendorUserId));

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
        product.setStatus(ProductStatus.APPROVED);

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
            Long availableStock = inventoryRepository.getTotalAvailableQuantityForVariant(v.getVariantId());
            return ProductResponseDto.VariantResponseDto.builder()
                    .variantId(v.getVariantId())
                    .sku(v.getSku())
                    .variantName(v.getVariantName())
                    .attributesJson(v.getAttributesJson())
                    .price(v.getPrice())
                    .isActive(v.isActive())
                    .availableStock(availableStock != null ? availableStock.intValue() : 0)
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
                .variants(variantDtos)
                .imageUrls(imageUrls)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
