package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Category> findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();

    List<Category> findByParentCategoryIsNullOrderByDisplayOrderAsc();

    List<Category> findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);

    List<Category> findByParentCategoryIdOrderByDisplayOrderAsc(Long parentId);

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    long countByParentCategoryId(Long parentId);
}
