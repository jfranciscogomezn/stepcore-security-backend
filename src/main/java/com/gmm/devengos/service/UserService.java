package com.gmm.devengos.service;

import com.gmm.devengos.controller.dto.user.CreateUserRequest;
import com.gmm.devengos.controller.dto.user.UpdateUserRequest;
import com.gmm.devengos.controller.dto.user.UserResponse;
import com.gmm.devengos.controller.dto.user.UserStatusRequest;

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
