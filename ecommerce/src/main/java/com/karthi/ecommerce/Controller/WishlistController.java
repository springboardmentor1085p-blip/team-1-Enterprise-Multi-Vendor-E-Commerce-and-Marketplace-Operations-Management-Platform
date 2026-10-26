package com.karthi.ecommerce.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.WishlistItemRequestDto;
import com.karthi.ecommerce.DTO.WishlistResponseDto;
import com.karthi.ecommerce.Service.WishlistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * Add a product to the authenticated user's wishlist.
     */
    @PostMapping("/items")
    public ResponseEntity<WishlistResponseDto> addToWishlist(
            Authentication authentication,
            @Valid @RequestBody WishlistItemRequestDto request
    ) {
        return ResponseEntity.ok(wishlistService.addToWishlist(authentication.getName(), request.productId()));
    }

    /**
     * Retrieve the current authenticated user's wishlist.
     */
    @GetMapping
    public ResponseEntity<WishlistResponseDto> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlist(authentication.getName()));
    }

    /**
     * Remove an item from the authenticated user's wishlist.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<WishlistResponseDto> removeWishlistItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(wishlistService.removeWishlistItem(authentication.getName(), itemId));
    }
}
