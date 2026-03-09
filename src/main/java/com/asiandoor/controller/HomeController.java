package com.asiandoor.controller;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model) {
        model.addAttribute("category", category);
        model.addAttribute("search", search);
        // products list will be populated once the service layer is implemented
        model.addAttribute("products", Collections.emptyList());
        return "products";
    }
}
