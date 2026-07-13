package com.p5store.dto.response;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    String imageUrl,
    Long parentId
) {}
