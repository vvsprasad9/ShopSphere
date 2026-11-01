package com.shopsphere.dto.response.category;

import com.shopsphere.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentCategoryId;
    private String parentCategoryName;
    private boolean active;
    private Integer displayOrder;
    @Builder.Default
    private List<CategoryResponse> subCategories = new ArrayList<>();

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                .active(category.isActive())
                .displayOrder(category.getDisplayOrder())
                .subCategories(category.getSubCategories() != null ?
                        category.getSubCategories().stream().map(CategoryResponse::fromEntity).collect(Collectors.toList()) :
                        new ArrayList<>())
                .build();
    }
}
