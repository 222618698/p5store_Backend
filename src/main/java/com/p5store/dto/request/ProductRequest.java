package com.p5store.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotBlank String sku,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    BigDecimal compareAtPrice,
    @Min(0) int stockQuantity,
    String imageUrl,
    List<String> galleryImages,
    String brand,
    String unit,
    String badge,
    boolean featured,
    Boolean active,
    @NotNull Long categoryId
) {}
