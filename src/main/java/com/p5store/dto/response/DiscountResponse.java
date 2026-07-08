package com.p5store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DiscountResponse(
    UUID id,
    String code,
    String discountType,
    BigDecimal value,
    Integer usageLimit,
    int usageCount,
    LocalDateTime validTo,
    boolean active
) {}
