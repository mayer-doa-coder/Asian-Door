package com.asiandoor.controller;

import java.util.List;
import java.util.stream.Collectors;

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
 * Admin product management controller â€” /admin/**.
 *
 * Security: two layers of protection.
 *   1. URL-level  â€” SecurityConfig: .requestMatchers("/admin/**").hasRole("ADMIN")
 *   2. Method-level â€” @PreAuthorize("hasRole('ADMIN')") on this class (defence in depth)
 *
 * Routes:
 *   GET  /admin                      â†’ admin dashboard
 *   GET  /admin/products             â†’ product list
 *   GET  /admin/products/new         â†’ create form
 *   POST /admin/products             â†’ submit create
 *   GET  /admin/products/{id}/edit   â†’ edit form
 *   POST /admin/products/{id}        â†’ submit update
 *   POST /admin/products/{id}/delete â†’ delete
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")    // method-level guard â€” second line of defence
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // â”€â”€ Dashboard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping
    public String adminDashboard(Model model) {
        List<ProductDTO> all = productService.getAllProductDTOs();

        long lowStock    = all.stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 5)
                .count();
        long outOfStock  = all.stream()
                .filter(p -> p.getStock() != null && p.getStock() == 0)
                .count();
        long categories  = all.stream()
                .filter(p -> p.getCategory() != null)
                .map(ProductDTO::getCategory)
                .distinct()
                .count();

        // Most-recent 5 additions shown in the Recent Products panel (list is ASC id, take last 5)
        List<ProductDTO> recent = all.stream()
                .skip(Math.max(0, all.size() - 5))
                .collect(Collectors.toList());
        java.util.Collections.reverse(recent);

        model.addAttribute("totalProducts",  all.size());
        model.addAttribute("lowStockCount",  lowStock);
        model.addAttribute("outOfStockCount",outOfStock);
        model.addAttribute("categoryCount",  categories);
        model.addAttribute("recentProducts", recent);

        return "admin/dashboard";
    }

    // â”€â”€ List â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/products")
    public String listProducts(Model model) {
        List<ProductDTO> all = productService.getAllProductDTOs();

        long lowStock   = all.stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 5)
                .count();
        long outOfStock = all.stream()
                .filter(p -> p.getStock() != null && p.getStock() == 0)
                .count();

        model.addAttribute("products",       all);
        model.addAttribute("totalProducts",  all.size());
        model.addAttribute("lowStockCount",  lowStock);
        model.addAttribute("outOfStockCount",outOfStock);
        return "admin/products";
    }

    // â”€â”€ Create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Edit / Update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
