package com.p5store.service;

import com.p5store.dto.request.CartItemRequest;
import com.p5store.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, CartItemRequest request);
    CartResponse updateItem(Long userId, Long productId, int quantity);
    CartResponse removeItem(Long userId, Long productId);
    void clearCart(Long userId);
}
