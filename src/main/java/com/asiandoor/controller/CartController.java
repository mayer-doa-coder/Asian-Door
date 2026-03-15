package com.asiandoor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asiandoor.dto.CartActionResponseDTO;
import com.asiandoor.dto.CartItemDTO;
import com.asiandoor.dto.CartSummaryDTO;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.exception.ResourceNotFoundException;
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
    public ResponseEntity<CartActionResponseDTO> addToCart(@PathVariable Long productId,
                                                           @RequestParam(defaultValue = "1") int quantity,
                                                           Authentication authentication) {
        Long userId = currentUserId(authentication);
        cartService.addToCart(userId, productId, quantity);
        CartActionResponseDTO response = new CartActionResponseDTO();
        response.setSuccess(true);
        response.setMessage("Product added to cart.");
        response.setProductId(productId);
        response.setQuantity(quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove/{productId}")
    public ResponseEntity<CartActionResponseDTO> removeFromCart(@PathVariable Long productId,
                                                                Authentication authentication) {
        Long userId = currentUserId(authentication);
        cartService.removeFromCart(userId, productId);
        CartActionResponseDTO response = new CartActionResponseDTO();
        response.setSuccess(true);
        response.setMessage("Product removed from cart.");
        response.setProductId(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartSummaryDTO> getCart(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<CartItemDTO> items = cartService.getCartItems(userId);
        double total = items.stream()
                .map(CartItemDTO::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        CartSummaryDTO response = new CartSummaryDTO();
        response.setItems(items);
        response.setTotalAmount(total);
        response.setItemCount(items.size());

        return ResponseEntity.ok(response);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        UserDTO user = userService.findUserDTOByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        return user.getId();
    }
}
