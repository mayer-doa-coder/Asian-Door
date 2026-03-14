package com.asiandoor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.asiandoor.entity.Order;
import com.asiandoor.entity.Product;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.OrderItemRepository;
import com.asiandoor.repository.OrderRepository;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.RoleRepository;
import com.asiandoor.repository.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class OrderCartApiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_CUSTOMER");
            return roleRepository.save(role);
        });

        Optional<User> existing = userRepository.findByEmail("customer@test.com");
        if (existing.isPresent()) {
            testUser = existing.get();
        } else {
            User user = new User();
            user.setName("Test Customer");
            user.setEmail("customer@test.com");
            user.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.Hqf4v2y2Q/JY9hw3rroWAt4EvsC0f6");
            user.setRole(customerRole);
            testUser = userRepository.save(user);
        }

        Product product = new Product();
        product.setName("Integration Test Door");
        product.setCategory("wooden");
        product.setMaterial("Oak");
        product.setPrice(1000.0);
        product.setDimensions("210 x 90 cm");
        product.setStock(10);
        product.setImageUrl("/images/products/test-door.jpg");
        product.setDescription("Integration testing product.");
        testProduct = productRepository.save(product);
    }

    @Test
    void shouldConvertCartToOrderAndPersistOrderTables() throws Exception {
        SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor authUser =
                SecurityMockMvcRequestPostProcessors.user("customer@test.com").roles("CUSTOMER");

        mockMvc.perform(post("/cart/add/{productId}", testProduct.getId())
                        .param("quantity", "2")
                        .with(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/cart").with(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(post("/orders").with(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/orders/my").with(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)));

        var orders = orderRepository.findByUserId(testUser.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orders.size());

        Order savedOrder = orders.get(0);
        var orderItems = orderItemRepository.findByOrderId(savedOrder.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orderItems.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, orderItems.get(0).getQuantity());

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(8, updatedProduct.getStock());

        mockMvc.perform(get("/cart").with(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
