package com.p5store.repository;

import com.p5store.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    List<Category> findByIsActiveTrue();
    List<Category> findByParentIsNull();
    long countByParent_Id(Long parentId);

    // Self-referencing parent FK must be cleared before a bulk delete of all
    // categories, otherwise deleting a parent while children still reference
    // it violates the FK constraint.
    @Modifying
    @Query("UPDATE Category c SET c.parent = NULL WHERE c.parent IS NOT NULL")
    void detachAllParents();
}
