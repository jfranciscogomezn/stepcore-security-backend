package com.stepcore.security.service;

import com.stepcore.security.controller.dto.role.CreateRoleRequest;
import com.stepcore.security.controller.dto.role.MenuNodeIdsRequest;
import com.stepcore.security.controller.dto.role.MenuNodeResponse;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.controller.dto.role.UpdateRoleRequest;

import java.util.List;

public interface RoleService {
    List<RoleResponse> findAll();
    RoleResponse findById(Long id);
    RoleResponse create(CreateRoleRequest request);
    RoleResponse update(Long id, UpdateRoleRequest request);
    void delete(Long id);
    List<MenuTreeNode> getMenuCatalogue();
    List<MenuNodeResponse> getAssignedMenuNodes(Long id);
    RoleResponse assignMenuNodes(Long id, MenuNodeIdsRequest request);
}
