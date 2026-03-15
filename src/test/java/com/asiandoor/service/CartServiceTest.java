package com.asiandoor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.asiandoor.dto.CartItemDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.exception.ResourceNotFoundException;
import com.asiandoor.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void shouldAddToCartAndBuildSnapshot() {
        Long userId = 2L;
        Long productId = 30L;

        Product product = new Product();
        product.setId(productId);
        product.setName("Security Door");
        product.setPrice(300.0);
        product.setStock(5);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        cartService.addToCart(userId, productId, 2);

        Map<Long, Integer> snapshot = cartService.getCartSnapshot(userId);
        assertEquals(1, snapshot.size());
        assertEquals(2, snapshot.get(productId));
    }

    @Test
    void shouldRejectWhenAddingUnknownProductToCart() {
        Long userId = 2L;
        Long productId = 404L;

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cartService.addToCart(userId, productId, 1));

        assertEquals("Product not found: 404", ex.getMessage());
    }

    @Test
    void shouldRejectWhenRequestedQuantityExceedsStock() {
        Long userId = 2L;
        Long productId = 31L;

        Product product = new Product();
        product.setId(productId);
        product.setName("Glass Door");
        product.setPrice(200.0);
        product.setStock(2);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        cartService.addToCart(userId, productId, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cartService.addToCart(userId, productId, 2));

        assertEquals("Requested quantity exceeds available stock.", ex.getMessage());
    }

    @Test
    void shouldCalculateCartItemsTotals() {
        Long userId = 3L;
        Long productId = 88L;

        Product product = new Product();
        product.setId(productId);
        product.setName("Interior Door");
        product.setPrice(150.0);
        product.setStock(10);
        product.setImageUrl("/img/interior.jpg");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        cartService.addToCart(userId, productId, 3);

        List<CartItemDTO> items = cartService.getCartItems(userId);
        assertEquals(1, items.size());
        assertEquals(450.0, items.get(0).getLineTotal());
        assertEquals("Interior Door", items.get(0).getProductName());
    }
}
