package com.shopstack.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shopstack.Enum.ProductStatus;
import com.shopstack.Enum.VendorStatus;
import com.shopstack.DTO.PageResponse;
import com.shopstack.DTO.ProductRequestDto;
import com.shopstack.DTO.ProductResponseDto;
import com.shopstack.DTO.ProductUpdateRequestDto;
import com.shopstack.Model.Category;
import com.shopstack.Model.Inventory;
import com.shopstack.Model.Product;
import com.shopstack.Model.ProductImage;
import com.shopstack.Model.ProductVariant;
import com.shopstack.Model.StockMovement;
import com.shopstack.Model.User;
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

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductImageRepository imageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private ProductService productService;

    private User sampleUser;
    private Vendor sampleVendor;
    private Category sampleCategory;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(10L);
        sampleUser.setEmail("vendor@shopstack.com");

        sampleVendor = new Vendor();
        sampleVendor.setVendorId(1L);
        sampleVendor.setUser(sampleUser);
        sampleVendor.setBusinessName("Tech Galaxy Store");
        sampleVendor.setStatus(VendorStatus.APPROVED);

        sampleCategory = new Category();
        sampleCategory.setCategoryId(2L);
        sampleCategory.setName("Electronics");
        sampleCategory.setSlug("electronics");

        sampleProduct = new Product();
        sampleProduct.setProductId(100L);
        sampleProduct.setName("Wireless Noise Cancelling Headphones");
        sampleProduct.setDescription("Premium bluetooth headphones");
        sampleProduct.setVendor(sampleVendor);
        sampleProduct.setCategory(sampleCategory);
        sampleProduct.setStatus(ProductStatus.APPROVED);
        sampleProduct.setCreatedAt(OffsetDateTime.now());
    }

    // ─── Product Creation Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully create a product when valid vendor and category exist")
    void testCreateProduct_Success() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Wireless Noise Cancelling Headphones");
        dto.setDescription("Premium bluetooth headphones");
        dto.setCategoryId(2L);

        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.createProduct(10L, dto);

        assertNotNull(response);
        assertEquals(100L, response.getProductId());
        assertEquals("Wireless Noise Cancelling Headphones", response.getName());
        assertEquals("Tech Galaxy Store", response.getVendorBusinessName());
        assertEquals("Electronics", response.getCategoryName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should create product with variants, images, and initial stock")
    void testCreateProduct_WithVariantsAndImages_Success() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Wireless Noise Cancelling Headphones");
        dto.setCategoryId(2L);
        dto.setImageUrls(List.of("https://example.com/img1.jpg"));

        ProductRequestDto.VariantRequestDto varDto = new ProductRequestDto.VariantRequestDto();
        varDto.setSku("HP-BLK-01");
        varDto.setVariantName("Black Edition");
        varDto.setPrice(new BigDecimal("149.99"));
        varDto.setInitialStock(25);
        dto.setVariants(List.of(varDto));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(1L);
        warehouse.setName("Central Warehouse");

        ProductVariant savedVariant = new ProductVariant();
        savedVariant.setVariantId(50L);
        savedVariant.setSku("HP-BLK-01");
        savedVariant.setPrice(new BigDecimal("149.99"));

        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);
        when(warehouseRepository.findByIsActiveTrue()).thenReturn(List.of(warehouse));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(List.of(savedVariant));
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.createProduct(10L, dto);

        assertNotNull(response);
        verify(imageRepository).save(any(ProductImage.class));
        verify(variantRepository).save(any(ProductVariant.class));
        verify(inventoryRepository).save(any(Inventory.class));
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when vendor profile is not found")
    void testCreateProduct_VendorNotFound() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Test Product");
        dto.setCategoryId(2L);

        when(vendorRepository.findByUserUserId(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(999L, dto);
        });

        assertEquals("Vendor profile not found for user: 999", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category is not found")
    void testCreateProduct_CategoryNotFound() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Test Product");
        dto.setCategoryId(999L);

        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(10L, dto);
        });

        assertEquals("Category not found: 999", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    // ─── Product Retrieval Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should retrieve product details by ID successfully")
    void testGetProductById_Success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.getProductById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getProductId());
        assertEquals("Wireless Noise Cancelling Headphones", response.getName());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product ID does not exist")
    void testGetProductById_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.getProductById(999L);
        });

        assertEquals("Product not found: 999", exception.getMessage());
    }

    @Test
    @DisplayName("Should only return APPROVED products for customer catalog browsing")
    void testGetAllProducts_ReturnsApprovedOnly() {
        when(productRepository.findByStatus(ProductStatus.APPROVED)).thenReturn(List.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<ProductResponseDto> results = productService.getAllProducts();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("APPROVED", results.get(0).getStatus());
        verify(productRepository).findByStatus(ProductStatus.APPROVED);
    }

    @Test
    @DisplayName("Should filter products by category ID")
    void testGetProductsByCategory_Success() {
        when(productRepository.findByCategory_CategoryId(2L)).thenReturn(List.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<ProductResponseDto> results = productService.getProductsByCategory(2L);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("Electronics", results.get(0).getCategoryName());
        verify(productRepository).findByCategory_CategoryId(2L);
    }

    @Test
    @DisplayName("Should search products by keyword")
    void testSearchProducts_Success() {
        when(productRepository.searchProducts("Headphones")).thenReturn(List.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<ProductResponseDto> results = productService.searchProducts("Headphones");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Wireless Noise Cancelling Headphones", results.get(0).getName());
        verify(productRepository).searchProducts("Headphones");
    }

    @Test
    @DisplayName("Should retrieve only products belonging to the logged-in vendor")
    void testGetVendorProducts_Success() {
        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(productRepository.findByVendor_VendorId(1L)).thenReturn(List.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<ProductResponseDto> results = productService.getVendorProducts(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Tech Galaxy Store", results.get(0).getVendorBusinessName());
        verify(productRepository).findByVendor_VendorId(1L);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when getting vendor products for non-existent vendor")
    void testGetVendorProducts_VendorNotFound() {
        when(vendorRepository.findByUserUserId(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.getVendorProducts(999L);
        });

        assertEquals("Vendor profile not found", exception.getMessage());
        verify(productRepository, never()).findByVendor_VendorId(any());
    }

    // ─── Milestone 2 Tests: Update, Soft Delete, Approvals & Security ────────────

    @Test
    @DisplayName("Should update product successfully by the product owner")
    void testUpdateProduct_Success() {
        ProductUpdateRequestDto dto = ProductUpdateRequestDto.builder()
                .name("Updated Wireless Headphones")
                .description("Updated description with ANC")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.updateProduct(10L, 100L, dto, false);

        assertNotNull(response);
        assertEquals(100L, response.getProductId());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Security: Vendor A must NOT be allowed to update Vendor B's product")
    void testUpdateProduct_UnauthorizedVendor_ThrowsAccessDenied() {
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(99L);
        otherVendor.setStatus(VendorStatus.APPROVED);

        ProductUpdateRequestDto dto = ProductUpdateRequestDto.builder()
                .name("Malicious Overwrite")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(vendorRepository.findByUserUserId(20L)).thenReturn(Optional.of(otherVendor));

        org.springframework.security.access.AccessDeniedException exception = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> productService.updateProduct(20L, 100L, dto, false)
        );

        assertEquals("You do not have permission to update this product", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Security: Variant IDOR protection - Cannot update variant belonging to another product")
    void testUpdateProduct_VariantIDOR_ThrowsAccessDenied() {
        Product anotherProduct = new Product();
        anotherProduct.setProductId(200L);

        ProductVariant foreignVariant = new ProductVariant();
        foreignVariant.setVariantId(77L);
        foreignVariant.setProduct(anotherProduct);

        ProductUpdateRequestDto.VariantUpdateRequestDto varDto = ProductUpdateRequestDto.VariantUpdateRequestDto.builder()
                .variantId(77L)
                .price(new BigDecimal("10.00"))
                .build();

        ProductUpdateRequestDto dto = ProductUpdateRequestDto.builder()
                .variants(List.of(varDto))
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.findById(77L)).thenReturn(Optional.of(foreignVariant));

        org.springframework.security.access.AccessDeniedException exception = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> productService.updateProduct(10L, 100L, dto, false)
        );

        assertEquals("Variant 77 does not belong to this product", exception.getMessage());
    }

    @Test
    @DisplayName("Security: Unapproved or suspended vendor cannot create products")
    void testCreateProduct_UnapprovedVendor_ThrowsAccessDenied() {
        Vendor pendingVendor = new Vendor();
        pendingVendor.setVendorId(2L);
        pendingVendor.setStatus(VendorStatus.PENDING);

        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("New Gadget");
        dto.setCategoryId(2L);

        when(vendorRepository.findByUserUserId(11L)).thenReturn(Optional.of(pendingVendor));

        org.springframework.security.access.AccessDeniedException exception = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> productService.createProduct(11L, dto)
        );

        assertTrue(exception.getMessage().contains("Only approved vendors can create products"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should soft-delete product by setting status to INACTIVE")
    void testDeleteProduct_SoftDelete_Success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(vendorRepository.findByUserUserId(10L)).thenReturn(Optional.of(sampleVendor));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.deleteProduct(10L, 100L, false);

        assertNotNull(response);
        assertEquals(ProductStatus.INACTIVE, sampleProduct.getStatus());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Security: Vendor A must NOT be allowed to delete Vendor B's product")
    void testDeleteProduct_UnauthorizedVendor_ThrowsAccessDenied() {
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(99L);

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(vendorRepository.findByUserUserId(20L)).thenReturn(Optional.of(otherVendor));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.deleteProduct(20L, 100L, false));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Admin approval: should update product status to APPROVED with notes")
    void testUpdateProductStatus_Approve_Success() {
        sampleProduct.setStatus(ProductStatus.PENDING_APPROVAL);

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.updateProductStatus(100L, ProductStatus.APPROVED, "Verified catalog quality");

        assertNotNull(response);
        assertEquals(ProductStatus.APPROVED, sampleProduct.getStatus());
        assertEquals("Verified catalog quality", sampleProduct.getAdminNotes());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Admin approval: should update product status to REJECTED with rejection note")
    void testUpdateProductStatus_Reject_Success() {
        sampleProduct.setStatus(ProductStatus.PENDING_APPROVAL);

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        ProductResponseDto response = productService.updateProductStatus(100L, ProductStatus.REJECTED, "Missing required specifications");

        assertNotNull(response);
        assertEquals(ProductStatus.REJECTED, sampleProduct.getStatus());
        assertEquals("Missing required specifications", sampleProduct.getAdminNotes());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Admin approval: should list all products pending review")
    void testGetPendingProducts_Success() {
        sampleProduct.setStatus(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findByStatus(ProductStatus.PENDING_APPROVAL)).thenReturn(List.of(sampleProduct));
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<ProductResponseDto> pending = productService.getPendingProducts();

        assertNotNull(pending);
        assertEquals(1, pending.size());
        assertEquals("PENDING_APPROVAL", pending.get(0).getStatus());
    }

    @Test
    @DisplayName("Pagination & Filtering: should return PageResponse with products")
    void testGetProductsWithFilters_Pagination() {
        org.springframework.data.domain.Page<Product> page = new org.springframework.data.domain.PageImpl<>(
                List.of(sampleProduct), org.springframework.data.domain.PageRequest.of(0, 10), 1
        );

        when(productRepository.findByStatus(eq(ProductStatus.APPROVED), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(variantRepository.findByProduct_ProductId(100L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByProduct_ProductIdOrderBySortOrderAsc(100L)).thenReturn(Collections.emptyList());

        PageResponse<ProductResponseDto> response = productService.getProductsWithFilters(
                null, null, null, null, null, false, org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPageNumber());
        assertEquals(1, response.getTotalElements());
    }
}
