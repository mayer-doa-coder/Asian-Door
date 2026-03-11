package com.asiandoor.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.asiandoor.dto.ProductDTO;
import com.asiandoor.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin product management controller — /admin/**.
 *
 * Security: two layers of protection.
 *   1. URL-level  — SecurityConfig: .requestMatchers("/admin/**").hasRole("SELLER")
 *   2. Method-level — @PreAuthorize("hasRole('SELLER')") on this class (defence in depth)
 *
 * Routes:
 *   GET  /admin                      → redirect to /admin/products
 *   GET  /admin/products             → product list
 *   GET  /admin/products/new         → create form
 *   POST /admin/products             → submit create
 *   GET  /admin/products/{id}/edit   → edit form
 *   POST /admin/products/{id}        → submit update
 *   POST /admin/products/{id}/delete → delete
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SELLER')")   // method-level guard — second line of defence
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ── Admin root ────────────────────────────────────────────────────────────

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/products";
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProductDTOs());
        return "admin/products";
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("editMode", false);
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute ProductDTO productDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", false);
            return "admin/product-form";
        }
        productService.createProduct(productDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Product \"" + productDTO.getName() + "\" created successfully.");
        return "redirect:/admin/products";
    }

    // ── Edit / Update ─────────────────────────────────────────────────────────

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        return productService.getProductById(id)
                .map(dto -> {
                    model.addAttribute("productDTO", dto);
                    model.addAttribute("editMode", true);
                    return "admin/product-form";
                })
                .orElseGet(() -> "redirect:/admin/products");
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute ProductDTO productDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            productDTO.setId(id);
            model.addAttribute("editMode", true);
            return "admin/product-form";
        }
        productService.updateProduct(id, productDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Product \"" + productDTO.getName() + "\" updated successfully.");
        return "redirect:/admin/products";
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.getProductById(id).ifPresent(dto ->
            redirectAttributes.addFlashAttribute("successMessage",
                "Product \"" + dto.getName() + "\" deleted successfully.")
        );
        productService.deleteProduct(id);
        return "redirect:/admin/products";
    }
}
