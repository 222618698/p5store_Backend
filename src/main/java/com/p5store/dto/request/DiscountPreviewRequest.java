package com.p5store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DiscountPreviewRequest(
    @NotBlank String code,
    @NotNull BigDecimal subtotal
) {}
