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

            // Wooden Doors
            product("Pine Wood Entry Door", "wooden", "Pine Wood", 8500.00, "210 × 90 cm", 10,
                "/images/products/pine-wood-door.jpg",
                "A classic entry door handcrafted from natural pine wood. Features a smooth finish, strong frame, and excellent insulation properties ideal for Filipino homes."),

            product("Narra Solid Wood Door", "wooden", "Narra Wood", 14500.00, "210 × 90 cm", 6,
                "/images/products/pine-wood-door.jpg",
                "Premium Narra solid wood door with a rich grain pattern and warm reddish tone. Durable, termite-resistant, and adds timeless elegance to any entrance."),

            // Steel Doors
            product("Galvanized Steel Entry Door", "steel", "Galvanized Steel", 15900.00, "210 × 90 cm", 5,
                "/images/products/pine-wood-door.jpg",
                "Heavy-duty galvanized steel door built for high-traffic areas. Rust-resistant coating ensures long-term durability in tropical climates."),

            // Glass Doors
            product("Tempered Glass Panel Door", "glass", "Tempered Glass", 21500.00, "210 × 90 cm", 8,
                "/images/products/pine-wood-door.jpg",
                "Modern tempered glass door that floods interiors with natural light. Engineered for safety with shatter-resistant glass and an aluminium frame."),

            // Security Doors
            product("Multi-Point Lock Security Door", "security", "Steel Alloy", 28000.00, "210 × 90 cm", 4,
                "/images/products/pine-wood-door.jpg",
                "Advanced security door featuring a multi-point locking system, reinforced steel core, and anti-tamper hinges. Certified for high-security residential use."),

            // Interior Doors
            product("Mahogany Panel Interior Door", "interior", "Mahogany Wood", 12300.00, "210 × 80 cm", 12,
                "/images/products/pine-wood-door.jpg",
                "Elegant mahogany interior door with a smooth, pre-finished surface. Lightweight yet sturdy, ideal for bedrooms, bathrooms, and living spaces."),

            // Exterior Doors
            product("Fiberglass Exterior Door", "exterior", "Fiberglass", 24000.00, "210 × 90 cm", 3,
                "/images/products/pine-wood-door.jpg",
                "Low-maintenance fiberglass exterior door engineered to resist warping, cracking, and corrosion. Achieves the look of real wood with superior weather performance.")
        ));
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
