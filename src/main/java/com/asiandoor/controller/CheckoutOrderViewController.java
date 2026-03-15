package com.asiandoor.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.asiandoor.dto.OrderDTO;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.exception.ResourceNotFoundException;
import com.asiandoor.service.OrderService;
import com.asiandoor.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CheckoutOrderViewController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/checkout")
    public String checkoutView() {
        return "checkout";
    }

    @GetMapping("/orders/history")
    public String ordersView(@RequestParam(required = false) Long placed,
                             Authentication authentication,
                             Model model) {
        Long userId = currentUserId(authentication);
        List<OrderDTO> orders = orderService.getOrdersByUser(userId);
        model.addAttribute("orders", orders);
        model.addAttribute("placedOrderId", placed);
        return "orders";
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
