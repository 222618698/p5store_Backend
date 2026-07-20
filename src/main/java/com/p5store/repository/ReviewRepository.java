package com.p5store.repository;

import com.p5store.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long countByProductIdAndApprovedTrue(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double averageRatingByProductId(Long productId);

    // Batched rating lookup for product listings — avoids issuing 2 separate
    // queries per product (average + count) when rendering a list, which
    // becomes a severe N+1 once listings return hundreds/thousands of rows.
    @Query("SELECT r.product.id AS productId, AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount " +
           "FROM Review r WHERE r.product.id IN :productIds AND r.approved = true GROUP BY r.product.id")
    List<ProductRatingAggregate> findRatingAggregatesByProductIds(@Param("productIds") List<Long> productIds);

    interface ProductRatingAggregate {
        Long getProductId();
        Double getAverageRating();
        Long getReviewCount();
    }
}
