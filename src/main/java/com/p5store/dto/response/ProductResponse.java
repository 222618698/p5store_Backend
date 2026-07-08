package com.p5store.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    String description,
    String sku,
    BigDecimal price,
    BigDecimal compareAtPrice,
    int stockQuantity,
    String imageUrl,
    List<String> galleryImages,
    String brand,
    String unit,
    String badge,
    boolean featured,
    String status,
    String categoryName,
    Long categoryId,
    Double averageRating,
    long reviewCount
) {}
