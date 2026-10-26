package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.karthi.ecommerce.DTO.WishlistResponseDto;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Wishlist;
import com.karthi.ecommerce.Model.WishlistItem;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.WishlistItemRepository;
import com.karthi.ecommerce.Repository.WishlistRepository;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("customer@shopstack.com");

        testProduct = new Product();
        testProduct.setProductId(20L);
        testProduct.setName("Mechanical Keyboard");
        testProduct.setPrice(new BigDecimal("129.99"));
        testProduct.setStockQuantity(5);
        testProduct.setActive(true);

        testWishlist = new Wishlist(testUser, "My Wishlist");
        testWishlist.setWishlistId(200L);
    }

    @Test
    void addToWishlist_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(20L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishlistResponseDto response = wishlistService.addToWishlist(testUser.getEmail(), 20L);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("Mechanical Keyboard", response.items().get(0).productName());
        assertTrue(response.items().get(0).inStock());
    }

    @Test
    void addToWishlist_PreventsDuplicates() {
        WishlistItem existingItem = new WishlistItem(testWishlist, testProduct);
        testWishlist.addItem(existingItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(20L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        WishlistResponseDto response = wishlistService.addToWishlist(testUser.getEmail(), 20L);

        assertNotNull(response);
        assertEquals(1, response.items().size(), "Should not create duplicate items for the same product");
    }

    @Test
    void addToWishlist_ThrowsException_WhenProductNotFound() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                wishlistService.addToWishlist(testUser.getEmail(), 99L));
    }

    @Test
    void removeWishlistItem_Success() {
        WishlistItem item = new WishlistItem(testWishlist, testProduct);
        item.setWishlistItemId(300L);
        testWishlist.addItem(item);

        when(wishlistItemRepository.findById(300L)).thenReturn(Optional.of(item));

        WishlistResponseDto response = wishlistService.removeWishlistItem(testUser.getEmail(), 300L);

        assertNotNull(response);
        assertEquals(0, response.items().size());
        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void removeWishlistItem_ThrowsAccessDenied_WhenNotOwner() {
        User otherUser = new User();
        otherUser.setEmail("other@shopstack.com");
        Wishlist otherWishlist = new Wishlist(otherUser, "Other Wishlist");

        WishlistItem item = new WishlistItem(otherWishlist, testProduct);
        item.setWishlistItemId(300L);

        when(wishlistItemRepository.findById(300L)).thenReturn(Optional.of(item));

        assertThrows(AccessDeniedException.class, () ->
                wishlistService.removeWishlistItem(testUser.getEmail(), 300L));
    }

    @Test
    void getWishlist_ReflectsStockAvailability() {
        testProduct.setStockQuantity(0); // Out of stock
        WishlistItem item = new WishlistItem(testWishlist, testProduct);
        testWishlist.addItem(item);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        WishlistResponseDto response = wishlistService.getWishlist(testUser.getEmail());

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertFalse(response.items().get(0).inStock(), "Should report product is not in stock");
    }
}
