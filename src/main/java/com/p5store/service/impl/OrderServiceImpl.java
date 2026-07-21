package com.p5store.service.impl;

import com.p5store.domain.*;
import com.p5store.enums.ProductStatus;
import com.p5store.dto.request.PlaceOrderRequest;
import com.p5store.dto.response.OrderResponse;
import com.p5store.dto.response.PayPalOrderResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.AddressRepository;
import com.p5store.repository.CartRepository;
import com.p5store.repository.OrderRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.DiscountService;
import com.p5store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal STANDARD_SHIPPING_COST = new BigDecimal("50.00");
    private static final Map<String, BigDecimal> FLAT_SHIPPING_COSTS = Map.of(
            "EXPRESS", new BigDecimal("150.00"),
            "SAME_DAY", new BigDecimal("350.00")
    );

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;
    private final PayPalService payPalService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        if (cart.getItems().isEmpty())
            throw new BusinessException("Cart is empty");

        Address address = addressRepository.findById(req.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + req.addressId()));
        if (!address.getUser().getId().equals(userId))
            throw new BusinessException("Address does not belong to this user");

        Order order = new Order();
        order.setOrderNumber("P5-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(cart.getUser());
        order.setShippingAddressSnapshot(formatAddress(address));
        order.setCouponCode(req.couponCode());
        order.setNotes(req.notes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();
            if (product.getStockQuantity() < qty)
                throw new BusinessException("Insufficient stock for: " + product.getName());

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setProductName(product.getName());
            oi.setProductSku(product.getSku());
            oi.setQuantity(qty);
            oi.setUnitPrice(product.getPrice());
            oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
            order.getItems().add(oi);

            product.setStockQuantity(product.getStockQuantity() - qty);
            if (product.getStockQuantity() == 0)
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            productRepository.save(product);

            subtotal = subtotal.add(oi.getSubtotal());
        }

        String shippingMethod = req.shippingMethod() == null || req.shippingMethod().isBlank()
                ? "STANDARD" : req.shippingMethod().toUpperCase();
        BigDecimal shipping = shippingCostFor(shippingMethod, subtotal);
        order.setShippingMethod(shippingMethod);

        BigDecimal discount = (req.couponCode() != null && !req.couponCode().isBlank())
                ? discountService.apply(req.couponCode(), subtotal)
                : BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setShippingCost(shipping);
        order.setDiscountAmount(discount);
        order.setTotal(subtotal.add(shipping).subtract(discount));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(Payment.PaymentMethod.valueOf(req.paymentMethod().toUpperCase()));
        payment.setStatus(Payment.PaymentStatus.PENDING);
        order.setPayment(payment);

        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Override
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdWithItems(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public OrderResponse getById(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Override
    public OrderResponse getByOrderNumber(String orderNumber) {
        return toResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber)));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = findOrder(orderId);
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED)
            throw new BusinessException("Order cannot be cancelled in status: " + order.getStatus());

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.getItems().forEach(oi -> {
            if (oi.getProduct() != null) {
                Product p = oi.getProduct();
                p.setStockQuantity(p.getStockQuantity() + oi.getQuantity());
                if (p.getStatus() == ProductStatus.OUT_OF_STOCK)
                    p.setStatus(ProductStatus.ACTIVE);
                productRepository.save(p);
            }
        });
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PayPalOrderResponse createPayPalOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");

        Payment payment = order.getPayment();
        if (payment == null || payment.getMethod() != Payment.PaymentMethod.PAYPAL)
            throw new BusinessException("This order is not set up for PayPal payment");
        if (payment.getStatus() != Payment.PaymentStatus.PENDING)
            throw new BusinessException("This order's payment is not pending");

        String returnUrl = frontendUrl + "/paypal/return?orderId=" + order.getId();
        String cancelUrl = frontendUrl + "/checkout?paypalCancelled=true";
        PayPalService.PayPalOrder created = payPalService.createOrder(
                order.getOrderNumber(), order.getTotal(), returnUrl, cancelUrl);

        payment.setPaypalOrderId(created.paypalOrderId());
        orderRepository.save(order);

        return new PayPalOrderResponse(created.approvalUrl());
    }

    @Override
    @Transactional
    public OrderResponse capturePayPalOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");

        Payment payment = order.getPayment();
        if (payment == null || payment.getPaypalOrderId() == null)
            throw new BusinessException("No PayPal order was started for this order");

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            return toResponse(order);
        }

        String captureId = payPalService.captureOrder(payment.getPaypalOrderId());
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId(captureId);
        payment.setGatewayResponse("PayPal capture completed");
        payment.setPaidAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CONFIRMED);

        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private BigDecimal shippingCostFor(String shippingMethod, BigDecimal subtotal) {
        if ("STANDARD".equals(shippingMethod)) {
            return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING_COST;
        }
        BigDecimal flat = FLAT_SHIPPING_COSTS.get(shippingMethod);
        if (flat == null) {
            throw new BusinessException("Unknown shipping method: " + shippingMethod);
        }
        return flat;
    }

    private String formatAddress(Address a) {
        return String.format("%s, %s, %s %s, %s", a.getStreet(), a.getCity(),
                a.getProvince() != null ? a.getProvince() + "," : "", a.getPostalCode(), a.getCountry());
    }

    OrderResponse toResponse(Order o) {
        List<OrderResponse.OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.getProductName(), i.getProductSku(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus().name(),
                o.getSubtotal(), o.getShippingCost(), o.getDiscountAmount(), o.getTotal(),
                o.getShippingAddressSnapshot(), o.getShippingMethod(), o.getCreatedAt(), items);
    }
}
