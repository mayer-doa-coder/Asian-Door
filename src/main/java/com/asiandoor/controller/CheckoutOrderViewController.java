package com.asiandoor.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public String checkoutView(Authentication authentication, Model model) {
        if (authentication != null && authentication.getName() != null) {
            userService.findUserDTOByEmail(authentication.getName())
                    .ifPresent(user -> model.addAttribute("user", user));
        }
        return "checkout";
    }

    @GetMapping("/orders/history")
    public String ordersHistoryRoot(@RequestParam(required = false) Long placed) {
        return placed != null
                ? "redirect:/orders/history/1?placed=" + placed
                : "redirect:/orders/history/1";
    }

    @GetMapping("/orders/history/{page}")
    public String ordersView(@PathVariable int page,
                             @RequestParam(required = false) Long placed,
                             Authentication authentication,
                             Model model) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        UserDTO user = userService.findUserDTOByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        Long userId = user.getId();
        int requestedPage = Math.max(page, 1);
        int pageSize = 4;

        Page<OrderDTO> orderPage = orderService.getOrdersByUser(userId, requestedPage - 1, pageSize);
        int totalPages = Math.max(orderPage.getTotalPages(), 1);

        if (requestedPage > totalPages) {
            return "redirect:/orders/history/" + totalPages;
        }

        model.addAttribute("user", user);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("placedOrderId", placed);
        model.addAttribute("currentPage", requestedPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", orderPage.getTotalElements());
        return "orders";
    }

}
