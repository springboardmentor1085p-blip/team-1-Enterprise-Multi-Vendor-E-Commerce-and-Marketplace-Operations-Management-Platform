package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.AddToCartRequest;
import com.karthi.ecommerce.DTO.CartResponseDto;
import com.karthi.ecommerce.DTO.UpdateCartItemRequest;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.ProductVariant;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.CartItemRepository;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.InventoryRepository;
import com.karthi.ecommerce.Repository.ProductVariantRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository variantRepository,
                       UserRepository userRepository,
                       InventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUser_UserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setStatus("ACTIVE");
            cart.setUpdatedAt(OffsetDateTime.now());
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public CartResponseDto addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + request.getVariantId()));

        if (!variant.isActive()) {
            throw new IllegalArgumentException("This product variant is currently inactive");
        }

        Integer availableStock = inventoryRepository.getTotalAvailableQuantityForVariant(variant.getVariantId());
        if (availableStock == null || availableStock < request.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock (" + (availableStock == null ? 0 : availableStock) + ")");
        }

        CartItem item = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variant.getVariantId())
                .orElse(null);

        if (item != null) {
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > availableStock) {
                throw new IllegalArgumentException("Cannot add more. Exceeds total available stock (" + availableStock + ")");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        return getCart(userId);
    }

    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
        } else {
            Integer availableStock = inventoryRepository.getTotalAvailableQuantityForVariant(item.getVariant().getVariantId());
            if (availableStock != null && request.getQuantity() > availableStock) {
                throw new IllegalArgumentException("Quantity exceeds available stock (" + availableStock + ")");
            }
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        return getCart(userId);
    }

    @Transactional
    public CartResponseDto removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        cartItemRepository.delete(item);
        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        return getCart(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);
    }

    public CartResponseDto getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCart_CartId(cart.getCartId());

        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalItemsCount = 0;
        List<CartResponseDto.CartItemResponseDto> itemDtos = new ArrayList<>();

        for (CartItem ci : items) {
            BigDecimal price = ci.getVariant().getPrice();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
            grandTotal = grandTotal.add(itemTotal);
            totalItemsCount += ci.getQuantity();

            itemDtos.add(CartResponseDto.CartItemResponseDto.builder()
                    .cartItemId(ci.getCartItemId())
                    .variantId(ci.getVariant().getVariantId())
                    .sku(ci.getVariant().getSku())
                    .variantName(ci.getVariant().getVariantName())
                    .productId(ci.getVariant().getProduct().getProductId())
                    .productName(ci.getVariant().getProduct().getName())
                    .vendorId(ci.getVariant().getProduct().getVendor().getVendorId())
                    .vendorBusinessName(ci.getVariant().getProduct().getVendor().getBusinessName())
                    .unitPrice(price)
                    .quantity(ci.getQuantity())
                    .itemTotal(itemTotal)
                    .build());
        }

        return CartResponseDto.builder()
                .cartId(cart.getCartId())
                .userId(userId)
                .items(itemDtos)
                .totalPrice(grandTotal)
                .totalItems(totalItemsCount)
                .build();
    }
}
