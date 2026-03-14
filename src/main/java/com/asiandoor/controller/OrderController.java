package com.asiandoor.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asiandoor.entity.Order;
import com.asiandoor.entity.User;
import com.asiandoor.service.OrderService;
import com.asiandoor.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(Authentication authentication) {
        try {
            Long userId = currentUserId(authentication);
            Order order = orderService.placeOrder(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", order.getId(),
                    "status", order.getStatus().name(),
                    "totalPrice", order.getTotalPrice()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myOrders(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<Order> orders = orderService.getOrdersByUser(userId);

        List<Map<String, Object>> orderSummaries = orders.stream()
                .map(order -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", order.getId());
                    row.put("orderDate", order.getOrderDate());
                    row.put("totalPrice", order.getTotalPrice());
                    row.put("status", order.getStatus().name());
                    return row;
                })
                .toList();

        return ResponseEntity.ok(Map.of("orders", orderSummaries));
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
