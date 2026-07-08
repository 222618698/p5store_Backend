package com.p5store.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 6) String password,
    String phone
) {}
