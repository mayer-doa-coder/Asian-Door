package com.asiandoor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asiandoor.dto.OrderDTO;
import com.asiandoor.dto.OrderCreateResponseDTO;
import com.asiandoor.dto.OrderListResponseDTO;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.exception.ResourceNotFoundException;
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
    public ResponseEntity<OrderCreateResponseDTO> createOrder(Authentication authentication) {
        Long userId = currentUserId(authentication);
        OrderDTO order = orderService.placeOrder(userId);

        OrderCreateResponseDTO response = new OrderCreateResponseDTO();
        response.setSuccess(true);
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<OrderListResponseDTO> myOrders(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<OrderDTO> orders = orderService.getOrdersByUser(userId);
        OrderListResponseDTO response = new OrderListResponseDTO();
        response.setOrders(orders);
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
