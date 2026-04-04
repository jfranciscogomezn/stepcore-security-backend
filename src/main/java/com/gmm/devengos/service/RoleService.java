package com.gmm.devengos.service;

import com.gmm.devengos.controller.dto.role.CreateRoleRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionIdsRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionResponse;
import com.gmm.devengos.controller.dto.role.RoleResponse;
import com.gmm.devengos.controller.dto.role.UpdateRoleRequest;

import java.util.List;

public interface RoleService {
    List<RoleResponse> findAll();
    RoleResponse findById(Long id);
    RoleResponse create(CreateRoleRequest request);
    RoleResponse update(Long id, UpdateRoleRequest request);
    void delete(Long id);
    List<MenuOptionResponse> getMenuOptions(Long id);
    RoleResponse assignMenuOptions(Long id, MenuOptionIdsRequest request);
}
