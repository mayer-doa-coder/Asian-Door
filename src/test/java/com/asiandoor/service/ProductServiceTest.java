package com.asiandoor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.asiandoor.dto.ProductDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        ProductDTO request = new ProductDTO();
        request.setName("Wooden Door");
        request.setCategory("wooden");
        request.setMaterial("Oak");
        request.setPrice(450.0);
        request.setDimensions("210x90 cm");
        request.setStock(12);
        request.setImageUrl("/images/products/wood-door.jpg");
        request.setDescription("Premium wooden door");

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0, Product.class);
            saved.setId(101L);
            return saved;
        });

        ProductDTO result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("Wooden Door", result.getName());
        assertEquals("wooden", result.getCategory());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product persisted = captor.getValue();
        assertEquals("Wooden Door", persisted.getName());
        assertEquals(12, persisted.getStock());
    }

    @Test
    void shouldUpdateProduct() {
        Long id = 7L;

        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old Door");
        existing.setStock(2);

        ProductDTO update = new ProductDTO();
        update.setName("Updated Door");
        update.setCategory("security");
        update.setMaterial("Steel");
        update.setPrice(999.0);
        update.setDimensions("220x100 cm");
        update.setStock(9);
        update.setImageUrl("/uploads/new.jpg");
        update.setDescription("Updated description");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDTO result = productService.updateProduct(id, update);

        assertEquals(id, result.getId());
        assertEquals("Updated Door", result.getName());
        assertEquals("security", result.getCategory());
        assertEquals(9, result.getStock());
    }

    @Test
    void shouldDeleteProduct() {
        Long id = 99L;

        productService.deleteProduct(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void shouldThrowWhenUpdatingMissingProduct() {
        Long id = 123L;
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(id, new ProductDTO()));

        assertEquals("Product not found: 123", ex.getMessage());
    }
}
