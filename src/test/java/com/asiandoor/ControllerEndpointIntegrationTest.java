package com.asiandoor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class ControllerEndpointIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void shouldReturnHomePageForPublicUsers() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void shouldReturnProductsPageForPublicUsers() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"));
    }

    @Test
    void shouldReturnCartPageForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/cart/view").with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void shouldRedirectAnonymousUserToLoginWhenAccessingOrdersHistoryPage() throws Exception {
        mockMvc.perform(get("/orders/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldRedirectAnonymousUserToLoginWhenAccessingOrdersApi() throws Exception {
        mockMvc.perform(get("/orders/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldDenyCustomerAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin").with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
