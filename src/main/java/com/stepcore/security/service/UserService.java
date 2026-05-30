package com.stepcore.security.service;

import com.stepcore.security.controller.dto.user.CreateUserRequest;
import com.stepcore.security.controller.dto.user.UpdateUserRequest;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse findById(Long id);
    UserResponse create(CreateUserRequest request, String actorEmail);
    UserResponse update(Long id, UpdateUserRequest request, String actorEmail);
    void delete(Long id, String actorEmail);
    UserResponse setStatus(Long id, UserStatusRequest request, String actorEmail);
    void resetPassword(Long id, String actorEmail);
}
