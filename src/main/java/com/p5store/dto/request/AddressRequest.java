package com.p5store.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
    @NotBlank String street,
    @NotBlank String city,
    String province,
    @NotBlank String postalCode,
    @NotBlank String country,
    String type,
    Boolean isDefault
) {}
