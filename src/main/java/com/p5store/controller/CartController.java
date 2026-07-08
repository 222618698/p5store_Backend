package com.p5store.controller;

import com.p5store.dto.request.CartItemRequest;
import com.p5store.dto.response.CartResponse;
import com.p5store.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users/{userId}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@PathVariable Long userId, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @PatchMapping("/items/{productId}")
    public CartResponse updateItem(@PathVariable Long userId, @PathVariable Long productId,
                                   @RequestParam int quantity) {
        return cartService.updateItem(userId, productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
    }
}
