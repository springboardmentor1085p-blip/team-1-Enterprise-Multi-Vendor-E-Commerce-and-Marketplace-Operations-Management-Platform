package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.Optional;

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
import org.springframework.security.access.AccessDeniedException;

import com.karthi.ecommerce.DTO.CartItemRequestDto;
import com.karthi.ecommerce.DTO.CartResponseDto;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.CartItemRepository;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("customer@shopstack.com");
        testUser.setFullName("John Customer");

        testProduct = new Product();
        testProduct.setProductId(10L);
        testProduct.setName("Wireless Gaming Mouse");
        testProduct.setPrice(new BigDecimal("49.99"));
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        testCart = new Cart(testUser);
        testCart.setCartId(100L);
    }

    @Test
    void addToCart_Success_NewItem() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemRequestDto request = new CartItemRequestDto(10L, 2);
        CartResponseDto response = cartService.addToCart(testUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(2, response.totalItems());
        assertEquals(new BigDecimal("99.98"), response.grandTotal());
        assertEquals("Wireless Gaming Mouse", response.items().get(0).productName());
    }

    @Test
    void addToCart_Success_IncrementExistingProduct() {
        CartItem existingItem = new CartItem(testCart, testProduct, 1);
        testCart.addItem(existingItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemRequestDto request = new CartItemRequestDto(10L, 2);
        CartResponseDto response = cartService.addToCart(testUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(1, response.items().size(), "Should maintain single cart line for the same product");
        assertEquals(3, response.totalItems());
        assertEquals(new BigDecimal("149.97"), response.grandTotal());
    }

    @Test
    void addToCart_ThrowsException_WhenQuantityExceedsStock() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        CartItemRequestDto request = new CartItemRequestDto(10L, 15); // Stock is 10

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(testUser.getEmail(), request));
        assertEquals("Requested total quantity (15) exceeds available stock (10)", ex.getMessage());
    }

    @Test
    void addToCart_ThrowsException_WhenQuantityIsZeroOrNegative() {
        CartItemRequestDto request = new CartItemRequestDto(10L, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(testUser.getEmail(), request));
        assertEquals("Quantity must be greater than zero", ex.getMessage());
    }

    @Test
    void addToCart_ThrowsException_WhenProductInactive() {
        testProduct.setActive(false);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        CartItemRequestDto request = new CartItemRequestDto(10L, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(testUser.getEmail(), request));
        assertEquals("Product is currently unavailable", ex.getMessage());
    }

    @Test
    void updateCartItemQuantity_Success() {
        CartItem item = new CartItem(testCart, testProduct, 1);
        item.setCartItemId(500L);
        testCart.addItem(item);

        when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

        CartResponseDto response = cartService.updateCartItemQuantity(testUser.getEmail(), 500L, 4);

        assertNotNull(response);
        assertEquals(4, response.totalItems());
        assertEquals(new BigDecimal("199.96"), response.grandTotal());
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateCartItemQuantity_ThrowsAccessDenied_WhenUserDoesNotOwnCart() {
        User otherUser = new User();
        otherUser.setEmail("intruder@shopstack.com");
        Cart otherCart = new Cart(otherUser);

        CartItem item = new CartItem(otherCart, testProduct, 1);
        item.setCartItemId(500L);

        when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

        assertThrows(AccessDeniedException.class, () ->
                cartService.updateCartItemQuantity(testUser.getEmail(), 500L, 2));
    }

    @Test
    void removeCartItem_Success() {
        CartItem item = new CartItem(testCart, testProduct, 1);
        item.setCartItemId(500L);
        testCart.addItem(item);

        when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

        CartResponseDto response = cartService.removeCartItem(testUser.getEmail(), 500L);

        assertNotNull(response);
        assertEquals(0, response.items().size());
        assertEquals(0, response.totalItems());
        assertEquals(BigDecimal.ZERO, response.grandTotal());
        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCart_Success() {
        testCart.addItem(new CartItem(testCart, testProduct, 2));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        cartService.clearCart(testUser.getEmail());

        assertEquals(0, testCart.getItems().size());
        verify(cartRepository).save(testCart);
    }
}
