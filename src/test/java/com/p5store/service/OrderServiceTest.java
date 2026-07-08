package com.p5store.service;

import com.p5store.domain.*;
import com.p5store.enums.ProductStatus;
import com.p5store.dto.request.PlaceOrderRequest;
import com.p5store.dto.response.OrderResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.AddressRepository;
import com.p5store.repository.CartRepository;
import com.p5store.repository.OrderRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.impl.OrderServiceImpl;
import com.p5store.service.DiscountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock AddressRepository addressRepository;
    @Mock ProductRepository productRepository;
    @Mock DiscountService discountService;
    @InjectMocks OrderServiceImpl orderService;

    User user;
    Cart cart;
    Product product;
    Address address;
    PlaceOrderRequest placeReq;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Headphones");
        product.setSku("HP-001");
        product.setPrice(new BigDecimal("300.00"));
        product.setStockQuantity(5);
        product.setStatus(ProductStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>(List.of(item)));

        address = new Address();
        address.setId(5L);
        address.setUser(user);
        address.setStreet("123 Main St");
        address.setCity("Cape Town");
        address.setPostalCode("8000");
        address.setCountry("South Africa");

        placeReq = new PlaceOrderRequest(5L, null, null, "VISA", null);
    }

    @Test
    void placeOrder_success_stockDeducted() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(5L)).thenReturn(Optional.of(address));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(cartRepository.save(any())).thenReturn(cart);

        OrderResponse resp = orderService.placeOrder(1L, placeReq);

        assertThat(resp.total()).isEqualByComparingTo(new BigDecimal("650.00")); // 600 + 50 shipping
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void placeOrder_emptyCart_throws() {
        cart.getItems().clear();
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.placeOrder(1L, placeReq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void placeOrder_addressNotFound_throws() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(1L, placeReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_wrongUser_throws() {
        User other = new User();
        other.setId(99L);
        address.setUser(other);

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(5L)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> orderService.placeOrder(1L, placeReq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void placeOrder_insufficientStock_throws() {
        product.setStockQuantity(1);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(5L)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> orderService.placeOrder(1L, placeReq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void cancelOrder_success_stockRestored() {
        Order order = new Order();
        order.setId(100L);
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);

        OrderItem oi = new OrderItem();
        oi.setProduct(product);
        oi.setQuantity(2);
        oi.setProductName("Headphones");
        oi.setProductSku("HP-001");
        oi.setUnitPrice(new BigDecimal("300.00"));
        oi.setSubtotal(new BigDecimal("600.00"));
        order.setItems(new ArrayList<>(List.of(oi)));

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(order);

        OrderResponse resp = orderService.cancelOrder(1L, 100L);
        assertThat(resp.status()).isEqualTo("CANCELLED");
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }
}
