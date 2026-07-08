package com.p5store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Email @Size(max = 100) String email,
    @Size(max = 30) String phone,
    @Size(max = 150) String company,
    @NotBlank String message
) {}
