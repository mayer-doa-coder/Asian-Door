package com.asiandoor.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.asiandoor.dto.OrderDTO;
import com.asiandoor.entity.Order;
import com.asiandoor.entity.OrderStatus;
import com.asiandoor.entity.Product;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.exception.ResourceNotFoundException;
import com.asiandoor.repository.OrderRepository;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrder() {
        Long userId = 5L;
        Long productId = 10L;

        Role role = new Role();
        role.setName("ROLE_CUSTOMER");

        User user = new User();
        user.setId(userId);
        user.setEmail("buyer@test.com");
        user.setRole(role);

        Product product = new Product();
        product.setId(productId);
        product.setName("Test Door");
        product.setPrice(2500.0);
        product.setStock(8);

        Map<Long, Integer> snapshot = new LinkedHashMap<>();
        snapshot.put(productId, 2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartSnapshot(userId)).thenReturn(snapshot);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0, Order.class);
            saved.setId(999L);
            return saved;
        });

        OrderDTO result = orderService.placeOrder(userId);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(5000.0, result.getTotalPrice());
        assertEquals(userId, result.getUserId());

        assertEquals(6, product.getStock());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order persisted = orderCaptor.getValue();
        assertEquals(1, persisted.getItems().size());
        assertEquals(OrderStatus.PENDING, persisted.getStatus());

        verify(cartService).clearCart(userId);
    }

    @Test
    void shouldRejectCreateOrderWhenCartIsEmpty() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartSnapshot(userId)).thenReturn(Map.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(userId));

        assertEquals("Cannot place order with an empty cart.", ex.getMessage());
    }

    @Test
    void shouldRejectCreateOrderWhenUserMissing() {
        Long userId = 41L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(userId));

        assertEquals("User not found: 41", ex.getMessage());
    }

    @Test
    void shouldUpdateOrderStatus() {
        Long orderId = 55L;

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(eq(order))).thenReturn(order);

        Order updated = orderService.updateStatus(orderId, OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED, updated.getStatus());
        verify(orderRepository).save(order);
    }
}
