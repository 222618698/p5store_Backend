package com.p5store.service;

import com.p5store.dto.request.ReviewRequest;
import com.p5store.dto.response.RatingSummaryResponse;
import com.p5store.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(Long productId, ReviewRequest request);
    List<ReviewResponse> getProductReviews(Long productId);
    RatingSummaryResponse getRatingSummary(Long productId);
}
