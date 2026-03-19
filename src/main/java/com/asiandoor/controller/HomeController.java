package com.asiandoor.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.asiandoor.dto.ProductDTO;
import com.asiandoor.service.ProductService;
import com.asiandoor.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<ProductDTO> result = productService.getFilteredProducts(category, search, page, sort, minPrice, maxPrice);
        Map<String, Long> categoryCounts = productService.getCategoryCounts();

        model.addAttribute("category", category);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("products", result.getContent());
        model.addAttribute("currentPage", result.getNumber());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalProducts", result.getTotalElements());

        return "products";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        return productService.getProductById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("relatedProducts", productService.getRelatedProductDTOs(product.getId(), product.getCategory(), 4));
                    return "product-detail";
                })
                .orElse("redirect:/products");
    }

    @GetMapping("/cart/view")
    public String cartView() {
        return "cart";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        String email = principal != null ? principal.getUsername() : null;
        if (email != null) {
            userService.findUserDTOByEmail(email).ifPresent(user -> model.addAttribute("user", user));
        }
        return "profile";
    }
}
