package com.stepcore.security.controller.mapper;

import com.stepcore.security.controller.dto.role.MenuOptionResponse;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.domain.model.MenuOption;
import com.stepcore.security.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    MenuOptionResponse toMenuOptionResponse(MenuOption menuOption);

    @Mapping(target = "menuOptions", expression = "java(sortedMenuOptions(role))")
    RoleResponse toRoleResponse(Role role);

    default List<MenuOptionResponse> sortedMenuOptions(final Role role) {
        return role.getMenuOptions().stream()
                .sorted(Comparator.comparingInt(MenuOption::getSortOrder))
                .map(this::toMenuOptionResponse)
                .toList();
    }
}
