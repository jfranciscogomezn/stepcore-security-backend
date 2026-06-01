package com.stepcore.security.controller;

import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.role.CreateRoleRequest;
import com.stepcore.security.controller.dto.role.MenuNodeIdsRequest;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerIT extends BaseIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuNodeRepository menuNodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Role adminRole;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .withName("ADMIN").withDescription("Admin role")
                        .withMenuNodes(new HashSet<>()).build()));

        final User admin = userRepository.save(User.builder()
                .withFirstName("Admin").withLastName("IT")
                .withEmail("role_it_admin@example.com")
                .withPasswordHash(passwordEncoder.encode("Admin@1234!"))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(adminRole).withUpdatedAt(LocalDateTime.now()).build());

        final String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", "role_it_admin@example.com", "Admin@1234!"))))
                .andReturn().getResponse().getContentAsString();

        adminToken = objectMapper.readTree(loginResponse).at("/data/token").asText();
    }

    @Test
    void shouldListAllRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldCreateRole() throws Exception {
        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateRoleRequest("TEST_ROLE_IT", "Integration test role"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("TEST_ROLE_IT"));
    }

    @Test
    void shouldReturn409WhenDeletingRoleWithUsers() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/" + adminRole.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectModuleMenuNodeAssignment() throws Exception {
        final MenuNode payrollModule = menuNodeRepository.findByCode("PAYROLL").orElseThrow();
        final Role customRole = roleRepository.save(Role.builder()
                .withName("MENU_IT_ROLE").withDescription("Menu assignment test")
                .withMenuNodes(new HashSet<>()).build());

        mockMvc.perform(put("/api/v1/roles/" + customRole.getId() + "/menu-nodes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MenuNodeIdsRequest(java.util.List.of(payrollModule.getId())))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenNonAdminAccessesRoles() throws Exception {
        final Role employeeRole = roleRepository.save(Role.builder()
                .withName("EMPLOYEE_IT_TEST").withDescription("Employee")
                .withMenuNodes(new HashSet<>()).build());

        userRepository.save(User.builder()
                .withFirstName("Emp").withLastName("User")
                .withEmail("emp_it@example.com")
                .withPasswordHash(passwordEncoder.encode("Admin@1234!"))
                .withEnabled(true).withMustChangePassword(false)
                .withRole(employeeRole).withUpdatedAt(LocalDateTime.now()).build());

        final String empLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("legacy", "emp_it@example.com", "Admin@1234!"))))
                .andReturn().getResponse().getContentAsString();

        final String empToken = objectMapper.readTree(empLoginResponse).at("/data/token").asText();

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }
}
