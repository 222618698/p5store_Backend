package com.p5store.service.impl;

import com.p5store.domain.Cart;
import com.p5store.domain.CartItem;
import com.p5store.domain.Product;
import com.p5store.dto.request.CartItemRequest;
import com.p5store.dto.response.CartResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    public CartResponse getCart(Long userId) {
        return toResponse(findCart(userId));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest req) {
        Cart cart = findCart(userId);
        Product product = findProduct(req.productId());

        if (product.getStockQuantity() == 0)
            throw new BusinessException("Product is out of stock");

        cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(req.productId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + req.quantity();
                            if (newQty > product.getStockQuantity())
                                throw new BusinessException("Not enough stock");
                            existing.setQuantity(newQty);
                        },
                        () -> {
                            if (req.quantity() > product.getStockQuantity())
                                throw new BusinessException("Not enough stock");
                            CartItem item = new CartItem();
                            item.setCart(cart);
                            item.setProduct(product);
                            item.setQuantity(req.quantity());
                            cart.getItems().add(item);
                        }
                );

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long productId, int quantity) {
        Cart cart = findCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));

        if (quantity > item.getProduct().getStockQuantity())
            throw new BusinessException("Not enough stock");

        item.setQuantity(quantity);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = findCart(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = findCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart findCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartResponse.CartItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getProduct().getPrice(),
                        i.getQuantity(),
                        i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                )).toList();

        BigDecimal total = items.stream()
                .map(CartResponse.CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, total);
    }
}
