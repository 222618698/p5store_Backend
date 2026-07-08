package com.p5store.dto.response;

public record AddressResponse(
    Long id,
    String street,
    String city,
    String province,
    String postalCode,
    String country,
    boolean isDefault,
    String type
) {}
