package com.p5store.dto.request;

import com.p5store.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DiscountRequest(
    @NotBlank String code,
    @NotNull DiscountType discountType,
    @NotNull @DecimalMin("0.01") java.math.BigDecimal value,
    @Min(1) Integer usageLimit,
    LocalDateTime validTo,
    Boolean active
) {}
