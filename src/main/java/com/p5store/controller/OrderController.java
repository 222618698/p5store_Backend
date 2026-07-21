package com.p5store.controller;

import com.p5store.domain.Order;
import com.p5store.dto.request.PlaceOrderRequest;
import com.p5store.dto.response.OrderResponse;
import com.p5store.dto.response.PayPalOrderResponse;
import com.p5store.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/v1/users/{userId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@PathVariable Long userId, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(userId, request);
    }

    @GetMapping("/v1/users/{userId}/orders")
    public List<OrderResponse> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    @GetMapping("/v1/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> getAllOrders(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.getAllOrders(pageable);
    }

    @GetMapping("/v1/orders/{orderId}")
    public OrderResponse getById(@PathVariable Long orderId) {
        return orderService.getById(orderId);
    }

    @GetMapping("/v1/orders/number/{orderNumber}")
    public OrderResponse getByOrderNumber(@PathVariable String orderNumber) {
        return orderService.getByOrderNumber(orderNumber);
    }

    @PatchMapping("/v1/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(@PathVariable Long orderId, @RequestParam Order.OrderStatus status) {
        return orderService.updateStatus(orderId, status);
    }

    @PostMapping("/v1/users/{userId}/orders/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.cancelOrder(userId, orderId);
    }

    @PostMapping("/v1/users/{userId}/orders/{orderId}/paypal/create")
    public PayPalOrderResponse createPayPalOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.createPayPalOrder(userId, orderId);
    }

    @PostMapping("/v1/users/{userId}/orders/{orderId}/paypal/capture")
    public OrderResponse capturePayPalOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.capturePayPalOrder(userId, orderId);
    }
}
