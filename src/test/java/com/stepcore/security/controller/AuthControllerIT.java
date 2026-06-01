package com.stepcore.security.controller;

import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private static final String TEST_EMAIL = "it_auth@example.com";
    private static final String TEST_PASSWORD = "Admin@1234!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        final Role role = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .withName("ADMIN").withDescription("Test admin")
                        .withMenuNodes(new HashSet<>()).build()));

        userRepository.save(User.builder()
                .withFirstName("Integration").withLastName("Test")
                .withEmail(TEST_EMAIL)
                .withPasswordHash(passwordEncoder.encode(TEST_PASSWORD))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(role).withUpdatedAt(LocalDateTime.now()).build());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL));
    }

    @Test
    void shouldReturnModularMenuTreeForAdminLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menu[*].code")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "SECURITY", "PAYROLL", "TIME_TRACKING", "ACCOUNT")))
                .andExpect(jsonPath("$.data.menu[?(@.code=='PAYROLL')].children[0].code")
                        .value("PAYROLL_CONFIG_GRP"))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions")
                        .value(org.hamcrest.Matchers.hasItems(
                                "EMPLOYEE_CONFIG", "PAYROLL_CONFIG", "ROLE_MANAGEMENT")));
    }

    @Test
    void shouldReturnLimitedMenuTreeForEmployeeLogin() throws Exception {
        final Role employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("EMPLOYEE role not seeded"));

        userRepository.save(User.builder()
                .withFirstName("Employee").withLastName("User")
                .withEmail("it_employee@example.com")
                .withPasswordHash(passwordEncoder.encode(TEST_PASSWORD))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(employeeRole).withUpdatedAt(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", "it_employee@example.com", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menu[*].code")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("TIME_TRACKING", "ACCOUNT")))
                .andExpect(jsonPath("$.data.permissions")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("MY_TIME", "MY_PROFILE")));
    }

    @Test
    void shouldEmbedLegacyAdminPermissionsInJwt() throws Exception {
        final String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", TEST_EMAIL, TEST_PASSWORD))))
                .andReturn().getResponse().getContentAsString();

        final String token = objectMapper.readTree(response).at("/data/token").asText();

        assertThat(jwtService.extractPermissions(token))
                .contains("EMPLOYEE_CONFIG", "PAYROLL_CONFIG", "ROLE_MANAGEMENT", "USER_MANAGEMENT");
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", TEST_EMAIL, "wrongPassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenLoginRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUserProfileWithValidToken() throws Exception {
        final String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", TEST_EMAIL, TEST_PASSWORD))))
                .andReturn().getResponse().getContentAsString();

        final String token = objectMapper.readTree(response).at("/data/token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL));
    }
}
