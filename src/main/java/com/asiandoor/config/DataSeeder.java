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

    @Value("${app.admin.name:Asian Door Admin}")
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
        if (productRepository.count() > 0) {
            normalizeExistingProductImageUrls();
            return;
        }

        productRepository.saveAll(List.of(

            // Wooden Doors
            product("Pine Wood Entry Door", "wooden", "Pine Wood", 8500.00, "210 × 90 cm", 10,
                "/images/country-house-door-pine-wood.jpg",
                "A classic entry door handcrafted from natural pine wood. Features a smooth finish, strong frame, and excellent insulation properties ideal for Filipino homes."),

            product("Narra Solid Wood Door", "wooden", "Narra Wood", 14500.00, "210 × 90 cm", 6,
                "/images/country-house-door-pine-wood.jpg",
                "Premium Narra solid wood door with a rich grain pattern and warm reddish tone. Durable, termite-resistant, and adds timeless elegance to any entrance."),

            // Steel Doors
            product("Galvanized Steel Entry Door", "steel", "Galvanized Steel", 15900.00, "210 × 90 cm", 5,
                "/images/country-house-door-pine-wood.jpg",
                "Heavy-duty galvanized steel door built for high-traffic areas. Rust-resistant coating ensures long-term durability in tropical climates."),

            // Glass Doors
            product("Tempered Glass Panel Door", "glass", "Tempered Glass", 21500.00, "210 × 90 cm", 8,
                "/images/country-house-door-pine-wood.jpg",
                "Modern tempered glass door that floods interiors with natural light. Engineered for safety with shatter-resistant glass and an aluminium frame."),

            // Security Doors
            product("Multi-Point Lock Security Door", "security", "Steel Alloy", 28000.00, "210 × 90 cm", 4,
                "/images/country-house-door-pine-wood.jpg",
                "Advanced security door featuring a multi-point locking system, reinforced steel core, and anti-tamper hinges. Certified for high-security residential use."),

            // Interior Doors
            product("Mahogany Panel Interior Door", "interior", "Mahogany Wood", 12300.00, "210 × 80 cm", 12,
                "/images/country-house-door-pine-wood.jpg",
                "Elegant mahogany interior door with a smooth, pre-finished surface. Lightweight yet sturdy, ideal for bedrooms, bathrooms, and living spaces."),

            // Exterior Doors
            product("Fiberglass Exterior Door", "exterior", "Fiberglass", 24000.00, "210 × 90 cm", 3,
                "/images/country-house-door-pine-wood.jpg",
                "Low-maintenance fiberglass exterior door engineered to resist warping, cracking, and corrosion. Achieves the look of real wood with superior weather performance.")
        ));

        normalizeExistingProductImageUrls();
    }

    private void normalizeExistingProductImageUrls() {
        String fallbackImage = "/images/country-house-door-pine-wood.jpg";

        productRepository.findAll().forEach(product -> {
            String imageUrl = product.getImageUrl();
            boolean changed = false;

            if (imageUrl == null || imageUrl.isBlank()) {
                product.setImageUrl(fallbackImage);
                changed = true;
            } else {
                String normalized = imageUrl.trim();

                if ("/images/products/pine-wood-door.jpg".equalsIgnoreCase(normalized)) {
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
