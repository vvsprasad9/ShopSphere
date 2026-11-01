package com.shopsphere.service;

import com.shopsphere.dto.request.category.CategoryRequest;
import com.shopsphere.dto.response.category.CategoryResponse;

import java.util.List;
import java.util.Set;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryBySlug(String slug);

    List<CategoryResponse> getCategoryHierarchy();

    List<CategoryResponse> getAllCategories(Boolean activeOnly);

    Set<Long> getAllDescendantCategoryIds(Long categoryId);
}
