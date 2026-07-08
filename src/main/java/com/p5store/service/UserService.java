package com.p5store.service;

import com.p5store.dto.request.ChangePasswordRequest;
import com.p5store.dto.request.LoginRequest;
import com.p5store.dto.request.RegisterRequest;
import com.p5store.dto.request.UpdateProfileRequest;
import com.p5store.dto.response.AuthResponse;
import com.p5store.dto.response.UserResponse;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getUser(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
