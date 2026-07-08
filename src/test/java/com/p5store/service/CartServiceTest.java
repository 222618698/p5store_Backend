package com.p5store.service;

import com.p5store.domain.*;
import com.p5store.enums.ProductStatus;
import com.p5store.dto.request.CartItemRequest;
import com.p5store.dto.response.CartResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks CartServiceImpl cartService;

    Cart cart;
    Product product;
    User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Laptop");
        product.setSku("LAP-001");
        product.setPrice(new BigDecimal("1200.00"));
        product.setStockQuantity(5);
        product.setStatus(ProductStatus.ACTIVE);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
    }

    @Test
    void addItem_success() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any())).thenReturn(cart);

        CartResponse resp = cartService.addItem(1L, new CartItemRequest(10L, 2));
        assertThat(resp.items()).hasSize(1);
    }

    @Test
    void addItem_mergesQuantity() {
        CartItem existing = new CartItem();
        existing.setProduct(product);
        existing.setQuantity(2);
        cart.getItems().add(existing);

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.addItem(1L, new CartItemRequest(10L, 2));
        assertThat(existing.getQuantity()).isEqualTo(4);
    }

    @Test
    void addItem_outOfStock_throws() {
        product.setStockQuantity(0);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemRequest(10L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    void addItem_exceedsStock_throws() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemRequest(10L, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    void addItem_productNotFound_throws() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_success() {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);
        cart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        CartResponse resp = cartService.removeItem(1L, 10L);
        assertThat(resp.items()).isEmpty();
    }

    @Test
    void clearCart_success() {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);
        cart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.clearCart(1L);
        assertThat(cart.getItems()).isEmpty();
    }
}
