package com.stepcore.security.service;

import com.stepcore.security.controller.dto.auth.ChangePasswordRequest;
import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.auth.LoginResponse;
import com.stepcore.security.controller.dto.user.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    UserResponse me(String email);
    void changePassword(String email, ChangePasswordRequest request);
}
