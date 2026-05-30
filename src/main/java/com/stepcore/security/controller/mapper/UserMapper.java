package com.stepcore.security.controller.mapper;

import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roleName", expression = "java(user.getRole().getName())")
    @Mapping(target = "roleId", expression = "java(user.getRole().getId())")
    UserResponse toUserResponse(User user);
}
