package com.p5store.repository;

import com.p5store.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Order items keep a name/SKU/price snapshot independent of the live
    // product row, so detaching (rather than deleting) preserves order
    // history when the underlying catalog is replaced.
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.product = NULL WHERE oi.product IS NOT NULL")
    void detachAllProducts();
}
