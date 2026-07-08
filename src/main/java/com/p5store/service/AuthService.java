package com.p5store.service;

import com.p5store.dto.request.AuthDtos.AuthResponse;
import com.p5store.dto.request.AuthDtos.LoginRequest;
import com.p5store.dto.request.AuthDtos.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
