package com.p5store.repository;

import com.p5store.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    List<Category> findByIsActiveTrue();
    List<Category> findByParentIsNull();
}
