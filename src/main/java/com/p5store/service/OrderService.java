package com.p5store.service;

import com.p5store.domain.Order;
import com.p5store.dto.request.PlaceOrderRequest;
import com.p5store.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(Long userId, PlaceOrderRequest request);
    List<OrderResponse> getUserOrders(Long userId);
    Page<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse getById(Long orderId);
    OrderResponse getByOrderNumber(String orderNumber);
    OrderResponse updateStatus(Long orderId, Order.OrderStatus status);
    OrderResponse cancelOrder(Long userId, Long orderId);
}
