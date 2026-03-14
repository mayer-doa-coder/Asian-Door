package com.asiandoor.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.asiandoor.dto.CartItemDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ProductRepository productRepository;

    private final Map<Long, Map<Long, Integer>> userCarts = new ConcurrentHashMap<>();

    public void addToCart(Long userId, Long productId, int quantity) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        int stock = product.getStock() == null ? 0 : product.getStock();
        if (stock <= 0) {
            throw new IllegalStateException("Product is out of stock.");
        }

        userCarts.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .compute(productId, (key, existingQty) -> {
                    int currentQty = existingQty == null ? 0 : existingQty;
                    int newQty = currentQty + quantity;
                    if (newQty > stock) {
                        throw new IllegalStateException("Requested quantity exceeds available stock.");
                    }
                    return newQty;
                });
    }

    public void removeFromCart(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return;
        }

        Map<Long, Integer> cart = userCarts.get(userId);
        if (cart == null) {
            return;
        }

        cart.remove(productId);
        if (cart.isEmpty()) {
            userCarts.remove(userId);
        }
    }

    public List<CartItemDTO> getCartItems(Long userId) {
        if (userId == null) {
            return List.of();
        }

        Map<Long, Integer> cart = userCarts.getOrDefault(userId, Map.of());
        if (cart.isEmpty()) {
            return List.of();
        }

        List<CartItemDTO> items = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                continue;
            }

            double unitPrice = product.getPrice() == null ? 0.0 : product.getPrice();
            CartItemDTO dto = new CartItemDTO();
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setImageUrl(product.getImageUrl());
            dto.setUnitPrice(unitPrice);
            dto.setQuantity(quantity);
            dto.setLineTotal(unitPrice * quantity);
            items.add(dto);
        }

        return items;
    }

    public void clearCart(Long userId) {
        if (userId != null) {
            userCarts.remove(userId);
        }
    }

    public Map<Long, Integer> getCartSnapshot(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        Map<Long, Integer> cart = userCarts.getOrDefault(userId, Map.of());
        return new LinkedHashMap<>(cart);
    }
}
