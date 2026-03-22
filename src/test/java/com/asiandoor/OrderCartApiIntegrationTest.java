package com.asiandoor;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
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

        @Autowired
        private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;
        private final String userRawPassword = "Password123!";

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
                        testUser.setPassword(passwordEncoder.encode(userRawPassword));
                        testUser.setVerified(true);
                        testUser.setVerificationCode(null);
                        testUser.setVerificationCodeExpiresAt(null);
                        testUser = userRepository.save(testUser);
        } else {
            User user = new User();
            user.setName("Test Customer");
            user.setEmail("customer@test.com");
                        user.setPassword(passwordEncoder.encode(userRawPassword));
            user.setRole(customerRole);
                        user.setVerified(true);
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
    void shouldRunCompletePurchaseWorkflowFromLoginToOrderHistory() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user(testUser.getEmail())
                        .password(userRawPassword))
                .andExpect(authenticated().withUsername(testUser.getEmail()))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/products").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Integration Test Door")));

        mockMvc.perform(post("/cart/add/{productId}", testProduct.getId())
                        .param("quantity", "2")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(post("/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/orders/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"));

        var orders = orderRepository.findByUserId(testUser.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orders.size());

        Order savedOrder = orders.get(0);
        var orderItems = orderItemRepository.findByOrderId(savedOrder.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orderItems.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, orderItems.get(0).getQuantity());

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(8, updatedProduct.getStock());

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void shouldRunFullPurchaseWorkflowFromRegisterToOrderHistory() throws Exception {
        String registeredEmail = "buyer+" + UUID.randomUUID() + "@test.com";
        String registeredPassword = "BuyerPass123!";

        MvcResult registerResult = mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Integration Buyer")
                        .param("email", registeredEmail)
                        .param("phone", "09171234567")
                        .param("password", registeredPassword)
                        .param("confirmPassword", registeredPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-signup"))
                .andReturn();

        MockHttpSession registerSession = (MockHttpSession) registerResult.getRequest().getSession(false);
        User registeredForVerification = userRepository.findByEmail(registeredEmail).orElseThrow();

        mockMvc.perform(post("/verify-signup")
                        .with(csrf())
                        .session(registerSession)
                        .param("code", registeredForVerification.getVerificationCode()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user(registeredEmail)
                        .password(registeredPassword))
                .andExpect(authenticated().withUsername(registeredEmail))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/products").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Integration Test Door")));

        mockMvc.perform(get("/products/{id}", testProduct.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"));

        mockMvc.perform(post("/cart/add/{productId}", testProduct.getId())
                        .param("quantity", "2")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(post("/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/orders/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"));

        User registeredUser = userRepository.findByEmail(registeredEmail).orElseThrow();
        var orders = orderRepository.findByUserId(registeredUser.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orders.size());

        Order savedOrder = orders.get(0);
        var orderItems = orderItemRepository.findByOrderId(savedOrder.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, orderItems.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, orderItems.get(0).getQuantity());

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(8, updatedProduct.getStock());

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void shouldAllowAdminPagesOnlyForAdminUsers() throws Exception {
        SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor customerUser =
                SecurityMockMvcRequestPostProcessors.user("customer@test.com").roles("CUSTOMER");

        mockMvc.perform(get("/admin").with(customerUser))
                .andExpect(status().isForbidden());

        MvcResult adminLogin = mockMvc.perform(formLogin("/login")
                        .user("admin@asiandoor.com")
                        .password("admin1234"))
                .andExpect(authenticated().withUsername("admin@asiandoor.com"))
                .andExpect(redirectedUrl("/admin"))
                .andReturn();

        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);

        mockMvc.perform(get("/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));

        mockMvc.perform(get("/admin/products").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"));

        mockMvc.perform(formLogin("/login")
                        .user("customer@test.com")
                        .password(userRawPassword))
                .andExpect(authenticated().withUsername("customer@test.com"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldReturnNotFoundFromGlobalHandlerWhenAddingMissingProductToCart() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user(testUser.getEmail())
                        .password(userRawPassword))
                .andExpect(authenticated().withUsername(testUser.getEmail()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/cart/add/{productId}", 999999L)
                        .param("quantity", "1")
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found: 999999"));
    }
}
