package com.p5store.dto.response;

public record AuthResponse(String token, Long userId, String email, String role) {}
