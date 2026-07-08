package com.p5store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    String status,
    BigDecimal subtotal,
    BigDecimal shippingCost,
    BigDecimal discountAmount,
    BigDecimal total,
    String shippingAddressSnapshot,
    String shippingMethod,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {
    public record OrderItemResponse(
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
