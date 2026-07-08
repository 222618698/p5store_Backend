package com.p5store.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    int rating,
    String title,
    String body,
    String reviewerName,
    LocalDateTime createdAt
) {}
