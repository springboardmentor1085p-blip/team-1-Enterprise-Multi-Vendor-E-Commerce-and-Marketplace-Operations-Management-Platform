package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.karthi.ecommerce.DTO.ProductRequestDto;
import com.karthi.ecommerce.DTO.ProductResponseDto;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.Role;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Model.VendorStatus;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private User vendorUser;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        Role vendorRole = new Role(1L, "ROLE_VENDOR");
        vendorUser = new User();
        vendorUser.setUserId(10L);
        vendorUser.setEmail("seller@shopstack.com");
        vendorUser.setFullName("Seller Jane");
        vendorUser.setRoles(Set.of(vendorRole));

        vendor = new Vendor();
        vendor.setVendorId(100L);
        vendor.setUser(vendorUser);
        vendor.setBusinessName("Jane's Gadgets");
        vendor.setStatus(VendorStatus.APPROVED);
    }

    @Test
    void createProduct_Success_WhenVendorIsApproved() {
        when(userRepository.findByEmail(vendorUser.getEmail())).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.of(vendor));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(1L);
            return p;
        });

        ProductRequestDto request = new ProductRequestDto("Wireless Mouse", "Ergonomic mouse", new BigDecimal("29.99"), 50);
        ProductResponseDto response = productService.createProduct(vendorUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(1L, response.productId());
        assertEquals("Wireless Mouse", response.name());
        assertEquals(new BigDecimal("29.99"), response.price());
        assertEquals(50, response.stockQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_ThrowsException_WhenVendorIsPending() {
        vendor.setStatus(VendorStatus.PENDING);
        when(userRepository.findByEmail(vendorUser.getEmail())).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.of(vendor));

        ProductRequestDto request = new ProductRequestDto("Wireless Mouse", "Ergonomic mouse", new BigDecimal("29.99"), 50);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(vendorUser.getEmail(), request));
        assertEquals("Vendor account is not approved. Current status: PENDING. Only approved vendors can create products.", ex.getMessage());
    }

    @Test
    void createProduct_ThrowsException_WhenNoVendorProfile() {
        vendorUser.setRoles(Collections.emptySet());
        when(userRepository.findByEmail(vendorUser.getEmail())).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUser(vendorUser)).thenReturn(Optional.empty());

        ProductRequestDto request = new ProductRequestDto("Wireless Mouse", "Ergonomic mouse", new BigDecimal("29.99"), 50);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(vendorUser.getEmail(), request));
        assertEquals("No vendor profile associated with this account. Only registered vendors can create products.", ex.getMessage());
    }
}
