package com.p5store.service;

import com.p5store.dto.request.DiscountRequest;
import com.p5store.dto.response.DiscountPreviewResponse;
import com.p5store.dto.response.DiscountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface DiscountService {
    DiscountResponse create(DiscountRequest request);
    DiscountResponse update(UUID id, DiscountRequest request);
    DiscountResponse setActive(UUID id, boolean active);
    Page<DiscountResponse> getAll(Pageable pageable);

    // Preview (read-only, does not consume usage) — used by the cart's
    // "Apply Promo Code" step before checkout is actually placed.
    DiscountPreviewResponse preview(String code, BigDecimal subtotal);

    // Validates + computes + increments usage count. Throws BusinessException
    // if the code is missing/inactive/expired/over its usage limit.
    BigDecimal apply(String code, BigDecimal subtotal);
}
