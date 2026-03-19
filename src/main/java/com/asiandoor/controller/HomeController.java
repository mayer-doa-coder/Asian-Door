package com.asiandoor.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;

import com.asiandoor.dto.ProfileUpdateRequest;
import com.asiandoor.dto.ProductDTO;
import com.asiandoor.dto.UserDTO;
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
            userService.findUserDTOByEmail(email).ifPresent(user -> {
                model.addAttribute("user", user);
                model.addAttribute("popularProducts", productService.getPopularProducts(4));
                model.addAttribute("recommendedProducts", productService.getRecommendedProductsForUser(user.getId(), 4));
            });
        }
        return "profile";
    }

    @GetMapping("/profile/update")
    public String profileUpdate(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam(required = false) Boolean updated,
                                Model model) {
        String email = principal != null ? principal.getUsername() : null;
        if (email == null) {
            return "redirect:/login";
        }

        UserDTO user = userService.findUserDTOByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/profile";
        }

        if (!model.containsAttribute("profileUpdateRequest")) {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setName(user.getName());
            request.setEmail(user.getEmail());
            request.setPassword("");
            request.setConfirmPassword("");
            model.addAttribute("profileUpdateRequest", request);
        }

        model.addAttribute("user", user);
        model.addAttribute("updated", Boolean.TRUE.equals(updated));
        return "profile-update";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                HttpServletRequest httpRequest,
                                HttpServletResponse httpResponse,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String currentEmail = principal != null ? principal.getUsername() : null;
        if (currentEmail == null) {
            return "redirect:/login";
        }

        UserDTO currentUser = userService.findUserDTOByEmail(currentEmail).orElse(null);
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("updated", false);
            return "profile-update";
        }

        try {
            boolean emailChanged = currentUser != null
                    && request.getEmail() != null
                    && !currentUser.getEmail().equalsIgnoreCase(request.getEmail().trim());
            boolean passwordChanged = request.getPassword() != null && !request.getPassword().trim().isEmpty();

            userService.updateProfile(currentEmail, request);

            if (emailChanged || passwordChanged) {
                if (authentication != null) {
                    new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
                }
                return "redirect:/login?logout";
            }

            redirectAttributes.addAttribute("updated", true);
            return "redirect:/profile/update";
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Unable to update profile.";
            if (message.toLowerCase().contains("email")) {
                bindingResult.rejectValue("email", "profile.update.email", message);
            } else if (message.toLowerCase().contains("retype") || message.toLowerCase().contains("match")) {
                bindingResult.rejectValue("confirmPassword", "profile.update.confirmPassword", message);
            } else if (message.toLowerCase().contains("password")) {
                bindingResult.rejectValue("password", "profile.update.password", message);
            } else if (message.toLowerCase().contains("name")) {
                bindingResult.rejectValue("name", "profile.update.name", message);
            } else {
                bindingResult.reject("profile.update", message);
            }
            model.addAttribute("updated", false);
            return "profile-update";
        }
    }
}
