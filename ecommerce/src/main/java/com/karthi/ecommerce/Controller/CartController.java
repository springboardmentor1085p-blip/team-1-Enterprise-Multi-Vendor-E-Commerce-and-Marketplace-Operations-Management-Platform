package com.karthi.ecommerce.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.DTO.CartItemRequestDto;
import com.karthi.ecommerce.DTO.CartResponseDto;
import com.karthi.ecommerce.DTO.UpdateCartItemRequestDto;
import com.karthi.ecommerce.Service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Add an item to the authenticated user's cart (or increment quantity if already exists).
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequestDto request
    ) {
        return ResponseEntity.ok(cartService.addToCart(authentication.getName(), request));
    }

    /**
     * Retrieve the current authenticated user's cart with computed subtotals and grand total.
     */
    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    /**
     * Update quantity for a specific item in the authenticated user's cart.
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateCartItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request
    ) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(authentication.getName(), itemId, request.quantity()));
    }

    /**
     * Remove a single item from the authenticated user's cart.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(cartService.removeCartItem(authentication.getName(), itemId));
    }

    /**
     * Clear all items from the authenticated user's cart.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
