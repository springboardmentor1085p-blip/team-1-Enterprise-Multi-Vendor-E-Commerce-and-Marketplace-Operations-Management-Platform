package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.CartItemRequestDto;
import com.karthi.ecommerce.DTO.CartItemResponseDto;
import com.karthi.ecommerce.DTO.CartResponseDto;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.CartItemRepository;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponseDto addToCart(String userEmail, CartItemRequestDto request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        User user = getUserByEmail(userEmail);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + request.productId()));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is currently unavailable");
        }

        Cart cart = getOrCreateCartEntity(user);

        // Check if product is already in the cart to avoid duplicate lines
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(product.getProductId()))
                .findFirst()
                .orElse(null);

        int targetQuantity = (existingItem != null)
                ? existingItem.getQuantity() + request.quantity()
                : request.quantity();

        if (targetQuantity > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Requested total quantity (" + targetQuantity + ") exceeds available stock (" + product.getStockQuantity() + ")"
            );
        }

        if (existingItem != null) {
            existingItem.setQuantity(targetQuantity);
        } else {
            CartItem newItem = new CartItem(cart, product, targetQuantity);
            cart.addItem(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToCartDto(savedCart);
    }

    @Transactional
    public CartResponseDto getCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        Cart cart = getOrCreateCartEntity(user);
        return mapToCartDto(cart);
    }

    @Transactional
    public CartResponseDto updateCartItemQuantity(String userEmail, Long itemId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with ID: " + itemId));

        // Enforce ownership: user can only modify items in their own cart
        if (!item.getCart().getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this cart item");
        }

        Product product = item.getProduct();
        if (newQuantity > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity (" + newQuantity + ") exceeds available stock (" + product.getStockQuantity() + ")"
            );
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return mapToCartDto(item.getCart());
    }

    @Transactional
    public CartResponseDto removeCartItem(String userEmail, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with ID: " + itemId));

        // Enforce ownership
        if (!item.getCart().getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this cart item");
        }

        Cart cart = item.getCart();
        cart.removeItem(item);
        cartItemRepository.delete(item);

        return mapToCartDto(cart);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        Cart cart = getOrCreateCartEntity(user);
        cart.clearItems();
        cartRepository.save(cart);
    }

    @Transactional
    public Cart getOrCreateCartEntity(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public CartResponseDto mapToCartDto(Cart cart) {
        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            itemDtos.add(new CartItemResponseDto(
                    item.getCartItemId(),
                    product.getProductId(),
                    product.getName(),
                    unitPrice,
                    item.getQuantity(),
                    lineTotal
            ));

            grandTotal = grandTotal.add(lineTotal);
            totalItems += item.getQuantity();
        }

        return new CartResponseDto(cart.getCartId(), itemDtos, totalItems, grandTotal);
    }
}
