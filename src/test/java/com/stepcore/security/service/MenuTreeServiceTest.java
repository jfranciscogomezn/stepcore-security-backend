package com.stepcore.security.service;

import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuTreeServiceTest {

    private MenuTreeService menuTreeService;

    private MenuNode securityModule;
    private MenuNode securityAdminGroup;
    private MenuNode roleManagement;
    private MenuNode userManagement;
    private MenuNode payrollModule;
    private MenuNode payrollConfigGroup;
    private MenuNode payrollConfig;
    private MenuNode employeeConfig;
    private MenuNode payrollOpsGroup;
    private MenuNode reports;
    private MenuNode accountModule;
    private MenuNode myProfile;

    @BeforeEach
    void setUp() {
        menuTreeService = new MenuTreeService();

        securityModule = node(1L, null, "SECURITY", "Security", MenuNodeType.MODULE, null, 10);
        securityAdminGroup = node(2L, 1L, "SECURITY_ADMIN", "Administration", MenuNodeType.GROUP, null, 10);
        roleManagement = node(3L, 2L, "ROLE_MANAGEMENT", "Role Management", MenuNodeType.ITEM, "/admin/roles", 10);
        userManagement = node(4L, 2L, "USER_MANAGEMENT", "User Management", MenuNodeType.ITEM, "/admin/users", 20);

        payrollModule = node(10L, null, "PAYROLL", "Payroll", MenuNodeType.MODULE, null, 20);
        payrollConfigGroup = node(11L, 10L, "PAYROLL_CONFIG_GRP", "Configuration", MenuNodeType.GROUP, null, 10);
        payrollConfig = node(12L, 11L, "PAYROLL_CONFIG", "Payroll Configuration", MenuNodeType.ITEM, "/admin/config", 10);
        employeeConfig = node(13L, 11L, "EMPLOYEE_CONFIG", "Employee Configuration", MenuNodeType.ITEM, "/admin/employees", 20);
        payrollOpsGroup = node(14L, 10L, "PAYROLL_OPS_GRP", "Operations", MenuNodeType.GROUP, null, 20);
        reports = node(15L, 14L, "REPORTS", "Reports", MenuNodeType.ITEM, "/reports", 10);

        accountModule = node(20L, null, "ACCOUNT", "Account", MenuNodeType.MODULE, null, 90);
        myProfile = node(21L, 20L, "MY_PROFILE", "My Profile", MenuNodeType.ITEM, "/my/profile", 10);
    }

    @Test
    void shouldBuildModularTreeForAdminAssignments() {
        final List<MenuNode> catalogue = List.of(
                securityModule, securityAdminGroup, roleManagement, userManagement,
                payrollModule, payrollConfigGroup, payrollConfig, employeeConfig, payrollOpsGroup, reports,
                accountModule, myProfile);

        final List<MenuNode> assigned = List.of(
                roleManagement, userManagement, payrollConfig, employeeConfig, reports, myProfile);

        final List<MenuTreeNode> tree = menuTreeService.buildTree(assigned, catalogue);

        assertThat(tree).extracting(MenuTreeNode::code)
                .containsExactly("SECURITY", "PAYROLL", "ACCOUNT");

        final MenuTreeNode payroll = tree.stream().filter(n -> "PAYROLL".equals(n.code())).findFirst().orElseThrow();
        assertThat(payroll.children()).extracting(MenuTreeNode::code)
                .containsExactly("PAYROLL_CONFIG_GRP", "PAYROLL_OPS_GRP");
        assertThat(payroll.children().stream()
                .filter(g -> "PAYROLL_CONFIG_GRP".equals(g.code()))
                .findFirst().orElseThrow()
                .children()).extracting(MenuTreeNode::code)
                .containsExactly("PAYROLL_CONFIG", "EMPLOYEE_CONFIG");
    }

    @Test
    void shouldOmitEmptyGroupsAndModules() {
        final List<MenuNode> catalogue = List.of(payrollModule, payrollConfigGroup, payrollConfig, payrollOpsGroup, reports);
        final List<MenuNode> assigned = List.of(payrollConfig);

        final List<MenuTreeNode> tree = menuTreeService.buildTree(assigned, catalogue);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).code()).isEqualTo("PAYROLL");
        assertThat(tree.get(0).children()).extracting(MenuTreeNode::code)
                .containsExactly("PAYROLL_CONFIG_GRP");
        assertThat(tree.get(0).children().get(0).children()).extracting(MenuTreeNode::code)
                .containsExactly("PAYROLL_CONFIG");
    }

    @Test
    void shouldBuildLimitedTreeForEmployeeAssignments() {
        final MenuNode timeModule = node(30L, null, "TIME_TRACKING", "Time Tracking", MenuNodeType.MODULE, null, 30);
        final MenuNode timeSelfGroup = node(31L, 30L, "TIME_SELF_GRP", "Self-service", MenuNodeType.GROUP, null, 10);
        final MenuNode myTime = node(32L, 31L, "MY_TIME", "My Time Records", MenuNodeType.ITEM, "/my/time", 10);

        final List<MenuNode> catalogue = List.of(timeModule, timeSelfGroup, myTime, accountModule, myProfile);
        final List<MenuNode> assigned = List.of(myTime, myProfile);

        final List<MenuTreeNode> tree = menuTreeService.buildTree(assigned, catalogue);

        assertThat(tree).extracting(MenuTreeNode::code).containsExactly("TIME_TRACKING", "ACCOUNT");
        assertThat(tree.get(0).children().get(0).children()).extracting(MenuTreeNode::code)
                .containsExactly("MY_TIME");
    }

    @Test
    void shouldExtractSortedPermissionsFromAssignedItems() {
        final List<MenuNode> assigned = List.of(employeeConfig, payrollConfig, roleManagement);

        assertThat(menuTreeService.extractPermissions(assigned))
                .containsExactly("EMPLOYEE_CONFIG", "PAYROLL_CONFIG", "ROLE_MANAGEMENT");
    }

    @Test
    void shouldExcludeDisabledItemsFromTreeAndPermissions() {
        final MenuNode disabled = MenuNode.builder()
                .withId(99L)
                .withParentId(11L)
                .withCode("DISABLED_ITEM")
                .withLabel("Disabled")
                .withNodeType(MenuNodeType.ITEM)
                .withRoute("/disabled")
                .withSortOrder(99)
                .withEnabled(false)
                .build();

        final List<MenuNode> catalogue = List.of(payrollModule, payrollConfigGroup, payrollConfig, disabled);
        final List<MenuNode> assigned = List.of(payrollConfig, disabled);

        assertThat(menuTreeService.extractPermissions(assigned)).containsExactly("PAYROLL_CONFIG");
        assertThat(menuTreeService.buildTree(assigned, catalogue)).hasSize(1);
    }

    private static MenuNode node(
            final Long id,
            final Long parentId,
            final String code,
            final String label,
            final MenuNodeType type,
            final String route,
            final int sortOrder) {
        return MenuNode.builder()
                .withId(id)
                .withParentId(parentId)
                .withCode(code)
                .withLabel(label)
                .withNodeType(type)
                .withRoute(route)
                .withSortOrder(sortOrder)
                .withEnabled(true)
                .build();
    }
}
