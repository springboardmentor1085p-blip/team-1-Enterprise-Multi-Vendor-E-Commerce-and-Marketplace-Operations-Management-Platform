package com.karthi.ecommerce.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.WishlistItemResponseDto;
import com.karthi.ecommerce.DTO.WishlistResponseDto;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Wishlist;
import com.karthi.ecommerce.Model.WishlistItem;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.WishlistItemRepository;
import com.karthi.ecommerce.Repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WishlistResponseDto addToWishlist(String userEmail, Long productId) {
        User user = getUserByEmail(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Wishlist wishlist = getOrCreateWishlistEntity(user);

        // Prevent duplicate entries for the same product in the user's wishlist
        boolean alreadyInWishlist = wishlist.getItems().stream()
                .anyMatch(item -> item.getProduct().getProductId().equals(product.getProductId()));

        if (!alreadyInWishlist) {
            WishlistItem item = new WishlistItem(wishlist, product);
            wishlist.addItem(item);
            wishlist = wishlistRepository.save(wishlist);
        }

        return mapToWishlistDto(wishlist);
    }

    @Transactional
    public WishlistResponseDto getWishlist(String userEmail) {
        User user = getUserByEmail(userEmail);
        Wishlist wishlist = getOrCreateWishlistEntity(user);
        return mapToWishlistDto(wishlist);
    }

    @Transactional
    public WishlistResponseDto removeWishlistItem(String userEmail, Long itemId) {
        WishlistItem item = wishlistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist item not found with ID: " + itemId));

        // Enforce ownership: user can only delete from their own wishlist
        if (!item.getWishlist().getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this wishlist item");
        }

        Wishlist wishlist = item.getWishlist();
        wishlist.removeItem(item);
        wishlistItemRepository.delete(item);

        return mapToWishlistDto(wishlist);
    }

    @Transactional
    public Wishlist getOrCreateWishlistEntity(User user) {
        return wishlistRepository.findByUser(user)
                .orElseGet(() -> wishlistRepository.save(new Wishlist(user, "My Wishlist")));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public WishlistResponseDto mapToWishlistDto(Wishlist wishlist) {
        List<WishlistItemResponseDto> items = new ArrayList<>();
        for (WishlistItem item : wishlist.getItems()) {
            Product product = item.getProduct();
            boolean inStock = product.isActive() && product.getStockQuantity() > 0;
            items.add(new WishlistItemResponseDto(
                    item.getWishlistItemId(),
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    inStock,
                    item.getAddedAt()
            ));
        }
        return new WishlistResponseDto(wishlist.getWishlistId(), wishlist.getName(), items);
    }
}
