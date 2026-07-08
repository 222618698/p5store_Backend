package com.p5store.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    Long cartId,
    List<CartItemResponse> items,
    BigDecimal total
) {
    public record CartItemResponse(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
    ) {}
}
