package com.stepcore.security.controller.mapper;

import com.stepcore.security.controller.dto.role.MenuNodeResponse;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    MenuNodeResponse toMenuNodeResponse(MenuNode menuNode);

    @Mapping(target = "menuNodes", expression = "java(sortedMenuNodes(role))")
    RoleResponse toRoleResponse(Role role);

    default List<MenuNodeResponse> sortedMenuNodes(final Role role) {
        return role.getMenuNodes().stream()
                .filter(node -> node.getNodeType() == MenuNodeType.ITEM)
                .sorted(Comparator.comparingInt(MenuNode::getSortOrder))
                .map(this::toMenuNodeResponse)
                .toList();
    }
}
