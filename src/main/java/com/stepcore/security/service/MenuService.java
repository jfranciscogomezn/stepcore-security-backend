package com.stepcore.security.service;

import com.stepcore.security.controller.dto.menu.CreateMenuNodeRequest;
import com.stepcore.security.controller.dto.menu.MenuNodeAdminResponse;
import com.stepcore.security.controller.dto.menu.UpdateMenuNodeRequest;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;

import java.util.List;

public interface MenuService {

    List<MenuTreeNode> getCatalogue(boolean includeDisabled);

    MenuNodeAdminResponse create(CreateMenuNodeRequest request);

    MenuNodeAdminResponse update(Long id, UpdateMenuNodeRequest request);

    void delete(Long id);
}
