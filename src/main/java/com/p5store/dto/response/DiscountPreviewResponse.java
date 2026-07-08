package com.p5store.dto.response;

import java.math.BigDecimal;

public record DiscountPreviewResponse(boolean valid, BigDecimal discountAmount, String message) {}
