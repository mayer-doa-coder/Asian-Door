package com.asiandoor.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.asiandoor.entity.Product;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.RoleRepository;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds roles, a default seller account, and demo products on first startup.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedSellerAccount();
        seedProducts();
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    private void seedRoles() {
        if (roleRepository.count() > 0) return;
        roleRepository.saveAll(List.of(
            role("ROLE_SELLER"),
            role("ROLE_BUYER")
        ));
    }

    private Role role(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    // ── Default seller / admin account ───────────────────────────────────────

    private void seedSellerAccount() {
        if (userRepository.findByEmail("admin@asiandoor.com").isPresent()) return;
        Role sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseThrow(() -> new IllegalStateException("ROLE_SELLER not found"));
        User seller = new User();
        seller.setName("Asian Door Admin");
        seller.setEmail("admin@asiandoor.com");
        seller.setPassword(passwordEncoder.encode("admin1234"));
        seller.setRole(sellerRole);
        userRepository.save(seller);
    }

    // ── Demo products ─────────────────────────────────────────────────────────

    private void seedProducts() {
        if (productRepository.count() > 0) return;
        productRepository.saveAll(List.of(
            product("Pine Wood Entry Door",     "Pine Wood",          8500.00,  10, "/images/country-house-door-pine-wood.jpg"),
            product("Steel Security Door",      "Galvanized Steel",  15900.00,   5, "/images/country-house-door-pine-wood.jpg"),
            product("Tempered Glass Door",      "Tempered Glass",    21500.00,   8, "/images/country-house-door-pine-wood.jpg"),
            product("Mahogany Interior Door",   "Mahogany Wood",     12300.00,  12, "/images/country-house-door-pine-wood.jpg"),
            product("Aluminium Sliding Door",   "Aluminium Alloy",   18750.00,   6, "/images/country-house-door-pine-wood.jpg"),
            product("Fiberglass Exterior Door", "Fiberglass",        24000.00,   3, "/images/country-house-door-pine-wood.jpg")
        ));
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
