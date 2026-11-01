package com.shopsphere.service.impl;

import com.shopsphere.dto.request.category.CategoryRequest;
import com.shopsphere.dto.response.category.CategoryResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.service.CategoryService;
import com.shopsphere.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category with name: {}", request.getName());

        String slug = generateUniqueSlug(request.getName(), null);

        Category parent = null;
        if (request.getParentCategoryId() != null) {
            parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentCategoryId()));
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parentCategory(parent)
                .active(request.isActive())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        Category saved = categoryRepository.save(category);
        if (parent != null) {
            parent.getSubCategories().add(saved);
        }
        log.info("Category created successfully with id: {}, slug: {}", saved.getId(), saved.getSlug());
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Cycle Prevention
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new BadRequestException("A category cannot be its own parent");
            }

            Category newParent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentCategoryId()));

            validateNoCycle(category, newParent);
            category.setParentCategory(newParent);
        } else {
            category.setParentCategory(null);
        }

        // Slug update if name changed
        if (!category.getName().equalsIgnoreCase(request.getName().trim())) {
            String slug = generateUniqueSlug(request.getName().trim(), id);
            category.setSlug(slug);
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.isActive());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        Category updated = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", updated.getId());
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Attempting to delete category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Safeguard 1: Subcategory check
        long subCategoryCount = categoryRepository.countByParentCategoryId(id);
        if (subCategoryCount > 0) {
            throw new BadRequestException("Cannot delete category with active subcategories. Please reassign or delete subcategories first.");
        }

        // Safeguard 2: Product check
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BadRequestException("Cannot delete category with associated products. Please reassign or delete products first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryHierarchy() {
        List<Category> rootCategories = categoryRepository.findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();
        return rootCategories.stream()
                .map(this::mapCategoryTree)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Boolean activeOnly) {
        List<Category> categories;
        if (Boolean.TRUE.equals(activeOnly)) {
            categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
        } else {
            categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
        }

        return categories.stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getAllDescendantCategoryIds(Long categoryId) {
        Set<Long> descendantIds = new HashSet<>();
        descendantIds.add(categoryId);

        Queue<Long> queue = new LinkedList<>();
        queue.add(categoryId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<Category> children = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(currentId);
            for (Category child : children) {
                if (descendantIds.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }

        return descendantIds;
    }

    private CategoryResponse mapCategoryTree(Category category) {
        CategoryResponse response = CategoryResponse.fromEntity(category);
        List<Category> children = categoryRepository.findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryResponse> activeSubCategories = children.stream()
                    .map(this::mapCategoryTree)
                    .collect(Collectors.toList());
            response.setSubCategories(activeSubCategories);
        }
        return response;
    }

    private void validateNoCycle(Category categoryToMove, Category candidateParent) {
        Category current = candidateParent;
        while (current != null) {
            if (current.getId().equals(categoryToMove.getId())) {
                throw new BadRequestException("Circular hierarchy detected: category cannot have one of its descendants as a parent");
            }
            current = current.getParentCategory();
        }
    }

    private String generateUniqueSlug(String name, Long currentCategoryId) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            baseSlug = "category";
        }

        String candidateSlug = baseSlug;
        int counter = 1;

        while (true) {
            boolean exists;
            if (currentCategoryId == null) {
                exists = categoryRepository.existsBySlug(candidateSlug);
            } else {
                exists = categoryRepository.existsBySlugAndIdNot(candidateSlug, currentCategoryId);
            }

            if (!exists) {
                return candidateSlug;
            }

            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }
    }
}
