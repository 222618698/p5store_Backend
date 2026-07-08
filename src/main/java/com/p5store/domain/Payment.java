package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 200)
    private String transactionId;

    @Column(length = 500)
    private String gatewayResponse;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }

    public enum PaymentMethod { CARD, EFT, SNAPSCAN, PAYPAL, VISA, MASTERCARD, MAESTRO }
    public enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }
}
