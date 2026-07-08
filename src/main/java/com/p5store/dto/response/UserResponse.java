package com.p5store.dto.response;

public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String role,
    String avatarUrl,
    boolean newsletterOptIn,
    boolean offersOptIn,
    boolean smsOptIn
) {}
