package com.p5store.repository;

import com.p5store.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByProductIdAndStockQuantityGreaterThan(Long productId, int minStock);
}
