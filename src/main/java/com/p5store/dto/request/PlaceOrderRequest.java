package com.p5store.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
    @NotNull Long addressId,
    String couponCode,
    String notes,
    @NotNull String paymentMethod,
    String shippingMethod
) {}
