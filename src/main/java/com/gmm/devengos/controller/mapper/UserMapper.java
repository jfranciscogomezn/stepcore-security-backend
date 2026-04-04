package com.gmm.devengos.controller.mapper;

import com.gmm.devengos.controller.dto.user.UserResponse;
import com.gmm.devengos.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roleName", expression = "java(user.getRole().getName())")
    @Mapping(target = "roleId", expression = "java(user.getRole().getId())")
    UserResponse toUserResponse(User user);
}
