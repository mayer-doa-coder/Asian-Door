package com.asiandoor.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asiandoor.dto.CartItemDTO;
import com.asiandoor.entity.User;
import com.asiandoor.service.CartService;
import com.asiandoor.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/add/{productId}")
    public ResponseEntity<Map<String, Object>> addToCart(@PathVariable Long productId,
                                                         @RequestParam(defaultValue = "1") int quantity,
                                                         Authentication authentication) {
        try {
            Long userId = currentUserId(authentication);
            cartService.addToCart(userId, productId, quantity);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Product added to cart.",
                    "productId", productId,
                    "quantity", quantity
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/remove/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Long productId,
                                                               Authentication authentication) {
        try {
            Long userId = currentUserId(authentication);
            cartService.removeFromCart(userId, productId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Product removed from cart.",
                    "productId", productId
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<CartItemDTO> items = cartService.getCartItems(userId);
        double total = items.stream()
                .map(CartItemDTO::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("totalAmount", total);
        response.put("itemCount", items.size());

        return ResponseEntity.ok(response);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        return user.getId();
    }
}
