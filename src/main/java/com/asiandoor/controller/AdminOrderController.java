package com.asiandoor.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.asiandoor.dto.OrderDTO;
import com.asiandoor.entity.OrderStatus;
import com.asiandoor.service.OrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String orders(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        int requestedPage = Math.max(page, 1);
        int pageSize = 10;

        Page<OrderDTO> orderPage = orderService.getAllOrders(requestedPage - 1, pageSize);
        int totalPages = Math.max(orderPage.getTotalPages(), 1);

        if (requestedPage > totalPages) {
            return "redirect:/admin/orders?page=" + totalPages;
        }

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", requestedPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", orderPage.getTotalElements());
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") OrderStatus status,
                               @RequestParam(name = "page", defaultValue = "1") int page,
                               RedirectAttributes redirectAttributes) {
        orderService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " updated to " + status + ".");
        return "redirect:/admin/orders?page=" + Math.max(page, 1);
    }
}
