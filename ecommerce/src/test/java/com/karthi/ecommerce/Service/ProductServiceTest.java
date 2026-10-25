package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.karthi.Enum.ProductStatus;
import com.karthi.ecommerce.DTO.ProductRequestDto;
import com.karthi.ecommerce.DTO.ProductResponseDto;
import com.karthi.ecommerce.Model.Category;
import com.karthi.ecommerce.Model.Inventory;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.ProductImage;
import com.karthi.ecommerce.Model.ProductVariant;
import com.karthi.ecommerce.Model.StockMovement;
import com.karthi.ecommerce.Model.User;
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
}
