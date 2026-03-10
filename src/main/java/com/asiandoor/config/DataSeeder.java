package com.asiandoor.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.asiandoor.entity.Product;
import com.asiandoor.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds demo products on first startup if the products table is empty.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) return;

        List<Product> demos = List.of(
            product("Pine Wood Entry Door",     "Pine Wood",          8500.00,  10, "/images/country-house-door-pine-wood.jpg"),
            product("Steel Security Door",      "Galvanized Steel",  15900.00,   5, "/images/country-house-door-pine-wood.jpg"),
            product("Tempered Glass Door",      "Tempered Glass",    21500.00,   8, "/images/country-house-door-pine-wood.jpg"),
            product("Mahogany Interior Door",   "Mahogany Wood",     12300.00,  12, "/images/country-house-door-pine-wood.jpg"),
            product("Aluminium Sliding Door",   "Aluminium Alloy",   18750.00,   6, "/images/country-house-door-pine-wood.jpg"),
            product("Fiberglass Exterior Door", "Fiberglass",        24000.00,   3, "/images/country-house-door-pine-wood.jpg")
        );

        productRepository.saveAll(demos);
    }

    private Product product(String name, String material, double price, int stock, String imageUrl) {
        Product p = new Product();
        p.setName(name);
        p.setMaterial(material);
        p.setPrice(price);
        p.setStock(stock);
        p.setImageUrl(imageUrl);
        return p;
    }
}
