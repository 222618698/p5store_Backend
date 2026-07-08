package com.p5store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    String phone,
    String avatarUrl,
    Boolean newsletterOptIn,
    Boolean offersOptIn,
    Boolean smsOptIn
) {}
