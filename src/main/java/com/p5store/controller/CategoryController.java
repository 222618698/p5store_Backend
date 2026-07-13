package com.p5store.controller;

import com.p5store.domain.Category;
import com.p5store.dto.response.CategoryResponse;
import com.p5store.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getDescription(), c.getImageUrl(),
                c.getParent() != null ? c.getParent().getId() : null);
    }
}
