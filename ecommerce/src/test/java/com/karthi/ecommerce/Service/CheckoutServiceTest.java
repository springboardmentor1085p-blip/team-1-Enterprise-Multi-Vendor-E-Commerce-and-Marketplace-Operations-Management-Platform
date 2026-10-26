package com.karthi.ecommerce.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.karthi.ecommerce.DTO.CheckoutRequestDto;
import com.karthi.ecommerce.DTO.OrderResponseDto;
import com.karthi.ecommerce.Model.Cart;
import com.karthi.ecommerce.Model.CartItem;
import com.karthi.ecommerce.Model.Order;
import com.karthi.ecommerce.Model.OrderStatus;
import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.CartRepository;
import com.karthi.ecommerce.Repository.OrderItemRepository;
import com.karthi.ecommerce.Repository.OrderRepository;
import com.karthi.ecommerce.Repository.ProductRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("customer@shopstack.com");

        testProduct = new Product();
        testProduct.setProductId(15L);
        testProduct.setName("Noise Cancelling Headphones");
        testProduct.setPrice(new BigDecimal("199.99"));
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        testCart = new Cart(testUser);
        testCart.setCartId(100L);
    }

    @Test
    void checkout_Success_FullWorkflow() {
        CartItem cartItem = new CartItem(testCart, testProduct, 2);
        testCart.addItem(cartItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(15L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(1001L);
            return o;
        });

        CheckoutRequestDto request = new CheckoutRequestDto("123 Tech Blvd, Suite 400", "CREDIT_CARD");
        OrderResponseDto response = checkoutService.checkout(testUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(1001L, response.orderId());
        assertTrue(response.orderNumber().startsWith("ORD-"));
        assertEquals("CONFIRMED", response.status());
        assertEquals(new BigDecimal("399.98"), response.grandTotal());
        assertEquals("123 Tech Blvd, Suite 400", response.shippingAddress());

        // Verify inventory was decreased
        assertEquals(8, testProduct.getStockQuantity(), "Inventory must be decreased from 10 to 8");
        verify(productRepository).save(testProduct);

        // Verify cart was cleared
        assertEquals(0, testCart.getItems().size(), "Cart must be emptied after successful checkout");
        verify(cartRepository).save(testCart);

        // Verify order items saved with price snapshot
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("199.99"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("399.98"), response.items().get(0).lineTotal());
    }

    @Test
    void checkout_ThrowsException_WhenCartIsEmpty() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart)); // empty cart

        CheckoutRequestDto request = new CheckoutRequestDto("123 Main St", "COD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.checkout(testUser.getEmail(), request));
        assertEquals("Cannot checkout: your shopping cart is empty", ex.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void checkout_ThrowsException_WhenShippingAddressIsBlank() {
        CheckoutRequestDto request = new CheckoutRequestDto("   ", "COD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.checkout(testUser.getEmail(), request));
        assertEquals("Shipping address is required for checkout", ex.getMessage());
    }

    @Test
    void checkout_ThrowsException_WhenStockIsInsufficient() {
        testProduct.setStockQuantity(1); // Only 1 available
        CartItem cartItem = new CartItem(testCart, testProduct, 3); // Requested 3
        testCart.addItem(cartItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(15L)).thenReturn(Optional.of(testProduct));

        CheckoutRequestDto request = new CheckoutRequestDto("123 Main St", "COD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.checkout(testUser.getEmail(), request));
        assertTrue(ex.getMessage().contains("Insufficient stock"));

        // Verify inventory was NOT deducted
        assertEquals(1, testProduct.getStockQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_ThrowsException_WhenProductInactive() {
        testProduct.setActive(false);
        CartItem cartItem = new CartItem(testCart, testProduct, 1);
        testCart.addItem(cartItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(15L)).thenReturn(Optional.of(testProduct));

        CheckoutRequestDto request = new CheckoutRequestDto("123 Main St", "COD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.checkout(testUser.getEmail(), request));
        assertTrue(ex.getMessage().contains("is currently inactive"));
    }

    @Test
    void getOrderDetails_ThrowsAccessDenied_WhenUserDoesNotOwnOrder() {
        User otherUser = new User();
        otherUser.setEmail("stranger@shopstack.com");

        Order order = new Order();
        order.setOrderId(888L);
        order.setUser(otherUser);

        when(orderRepository.findById(888L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () ->
                checkoutService.getOrderDetails(testUser.getEmail(), 888L));
    }

    @Test
    void getMyOrders_ReturnsUserOrders() {
        Order order = new Order();
        order.setOrderId(100L);
        order.setOrderNumber("ORD-12345");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUser(testUser);
        order.setSubtotal(new BigDecimal("50.00"));
        order.setShippingFee(BigDecimal.ZERO);
        order.setGrandTotal(new BigDecimal("50.00"));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserOrderByPlacedAtDesc(testUser)).thenReturn(List.of(order));

        List<OrderResponseDto> orders = checkoutService.getMyOrders(testUser.getEmail());

        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("ORD-12345", orders.get(0).orderNumber());
    }
}
