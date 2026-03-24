package com.asiandoor.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.admin.name:Asian Wooden Decor Admin}")
    private String adminName;

    @Value("${app.admin.email:admin@asiandoor.com}")
    private String adminEmail;

    @Value("${app.admin.password:change_this_admin_password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminAccount();
        seedProducts();
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    private void seedRoles() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(role("ROLE_ADMIN"));
        }
        if (roleRepository.findByName("ROLE_CUSTOMER").isEmpty()) {
            roleRepository.save(role("ROLE_CUSTOMER"));
        }
    }

    private Role role(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    // ── Default admin account ────────────────────────────────────────────────

    private void seedAdminAccount() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

        userRepository.findByEmail(adminEmail)
                .ifPresentOrElse(existingAdmin -> {
                    existingAdmin.setName(adminName);
                    existingAdmin.setRole(adminRole);
                    existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
                    existingAdmin.setVerified(true);
                    existingAdmin.setVerificationCode(null);
                    existingAdmin.setVerificationCodeExpiresAt(null);
                    userRepository.save(existingAdmin);
                }, () -> {
                    User admin = new User();
                    admin.setName(adminName);
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(adminRole);
                    admin.setVerified(true);
                    userRepository.save(admin);
                });
    }

    // ── Demo products ─────────────────────────────────────────────────────────

    private void seedProducts() {
        List<Product> demoCatalog = List.of(

                product("Minimalist Coffee Table", "wooden", "Engineered Wood", 8500.00, "120 x 60 cm", 10,
                    "/images/products/coffee table.jpg",
                        "A clean-lined coffee table that anchors your living room with practical surface space and a warm wood finish."),

                product("Queen Size Wooden Bed", "wooden", "Solid Wood", 32500.00, "200 x 160 cm", 5,
                    "/images/products/bed 1.jpg",
                        "A queen-size bed frame with sturdy support and elegant detailing, designed for restful everyday comfort."),

                product("Premium Steel Almira", "security", "Powder-Coated Steel", 28000.00, "200 x 95 cm", 4,
                        "/images/products/almira.jpg",
                        "A spacious steel almira with secure locking and smart shelf layout for organized clothing and document storage."),

                product("Compact Office Almira", "security", "Steel Alloy", 15900.00, "190 x 80 cm", 6,
                        "/images/products/almira2.jpg",
                        "A compact office-ready almira with reinforced shelves that keeps files, essentials, and tools neatly arranged."),

                product("Family Dining Table Set", "interior", "Tempered Glass and Wood", 21500.00, "200 x 90 cm", 8,
                        "/images/products/table1.jpg",
                        "A six-seater dining table setup that balances premium finish, durable construction, and day-to-day practicality."),

                product("Modern Fabric Sofa", "interior", "Fabric Upholstery", 24000.00, "190 x 85 cm", 7,
                        "/images/products/sofa.jpg",
                        "A modern fabric sofa with supportive cushions and a clean silhouette to elevate family seating spaces."),

                product("Classic Accent Chair", "interior", "Mahogany Wood", 12300.00, "95 x 70 cm", 12,
                        "/images/products/chair-1.jpg",
                        "A refined accent chair ideal for reading corners and lounge areas, combining comfort with elegant style.")
        );

        demoCatalog.forEach(seedProduct -> productRepository.findByNameIgnoreCase(seedProduct.getName())
                .ifPresentOrElse(existing -> {
                    existing.setCategory(seedProduct.getCategory());
                    existing.setMaterial(seedProduct.getMaterial());
                    existing.setPrice(seedProduct.getPrice());
                    existing.setDimensions(seedProduct.getDimensions());
                    existing.setStock(seedProduct.getStock());
                    existing.setImageUrl(seedProduct.getImageUrl());
                    existing.setDescription(seedProduct.getDescription());
                    productRepository.save(existing);
                }, () -> productRepository.save(seedProduct)));

        normalizeExistingProductImageUrls();
    }

    private void normalizeExistingProductImageUrls() {
        String fallbackImage = "/images/products/sofa.jpg";

        productRepository.findAll().forEach(product -> {
            String imageUrl = product.getImageUrl();
            boolean changed = false;

            if (imageUrl == null || imageUrl.isBlank()) {
                product.setImageUrl(fallbackImage);
                changed = true;
            } else {
                String normalized = imageUrl.trim();

                if ("/images/products/pine-wood-door.jpg".equalsIgnoreCase(normalized)
                        || "/images/country-house-door-pine-wood.jpg".equalsIgnoreCase(normalized)) {
                    product.setImageUrl(fallbackImage);
                    changed = true;
                } else if (!normalized.startsWith("http://")
                        && !normalized.startsWith("https://")
                        && !normalized.startsWith("/")) {
                    product.setImageUrl("/" + normalized);
                    changed = true;
                }
            }

            if (changed) {
                productRepository.save(product);
            }
        });
    }

    private Product product(String name, String category, String material,
                            double price, String dimensions, int stock,
                            String imageUrl, String description) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setMaterial(material);
        p.setPrice(price);
        p.setDimensions(dimensions);
        p.setStock(stock);
        p.setImageUrl(imageUrl);
        p.setDescription(description);
        return p;
    }
}
