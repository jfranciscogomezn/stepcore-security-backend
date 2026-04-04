package com.gmm.devengos.service;

import com.gmm.devengos.controller.dto.auth.ChangePasswordRequest;
import com.gmm.devengos.controller.dto.auth.LoginRequest;
import com.gmm.devengos.controller.dto.auth.LoginResponse;
import com.gmm.devengos.controller.dto.user.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    UserResponse me(String email);
    void changePassword(String email, ChangePasswordRequest request);
}
