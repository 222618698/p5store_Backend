package com.p5store.dto.response;

import java.time.LocalDateTime;

public record ContactMessageResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String company,
    String message,
    LocalDateTime createdAt
) {}
