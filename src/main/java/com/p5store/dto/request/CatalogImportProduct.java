package com.p5store.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record CatalogImportProduct(
    String name,
    String description,
    String sku,
    BigDecimal price,
    BigDecimal compareAtPrice,
    Integer stockQuantity,
    String imageUrl,
    List<String> galleryImages,
    String categoryName,
    String parentCategoryName
) {}
