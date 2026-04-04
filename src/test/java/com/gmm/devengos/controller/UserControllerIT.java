package com.gmm.devengos.controller;

import com.gmm.devengos.controller.dto.auth.LoginRequest;
import com.gmm.devengos.controller.dto.user.CreateUserRequest;
import com.gmm.devengos.controller.dto.user.UserStatusRequest;
import com.gmm.devengos.domain.model.Role;
import com.gmm.devengos.domain.model.User;
import com.gmm.devengos.repository.RoleRepository;
import com.gmm.devengos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Role adminRole;
    private Role employeeRole;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .withName("ADMIN").withDescription("Admin")
                        .withMenuOptions(new HashSet<>()).build()));
        employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .withName("EMPLOYEE").withDescription("Employee")
                        .withMenuOptions(new HashSet<>()).build()));

        userRepository.save(User.builder()
                .withFirstName("Admin").withLastName("IT")
                .withEmail("user_it_admin@example.com")
                .withPasswordHash(passwordEncoder.encode("Admin@1234!"))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(adminRole).withUpdatedAt(LocalDateTime.now()).build());

        final String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user_it_admin@example.com", "Admin@1234!"))))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).at("/data/token").asText();
    }

    @Test
    void shouldListAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldCreateUser() throws Exception {
        final CreateUserRequest request = new CreateUserRequest(
                "New", "User", "newuser_it@example.com", null, "Admin@1234!", employeeRole.getId());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("newuser_it@example.com"));
    }

    @Test
    void shouldReturn409WhenCreatingUserWithDuplicateEmail() throws Exception {
        final CreateUserRequest request = new CreateUserRequest(
                "Admin", "IT", "user_it_admin@example.com", null, "Admin@1234!", adminRole.getId());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldToggleUserStatus() throws Exception {
        final User target = userRepository.save(User.builder()
                .withFirstName("Toggle").withLastName("Me")
                .withEmail("toggle_it@example.com")
                .withPasswordHash(passwordEncoder.encode("Admin@1234!"))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(employeeRole).withUpdatedAt(LocalDateTime.now()).build());

        mockMvc.perform(patch("/api/v1/users/" + target.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void shouldResetPassword() throws Exception {
        final User target = userRepository.save(User.builder()
                .withFirstName("Reset").withLastName("Me")
                .withEmail("reset_it@example.com")
                .withPasswordHash(passwordEncoder.encode("Admin@1234!"))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(employeeRole).withUpdatedAt(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/v1/users/" + target.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
