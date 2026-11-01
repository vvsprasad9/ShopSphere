package com.shopsphere.service;

import com.shopsphere.dto.request.category.CategoryRequest;
import com.shopsphere.dto.response.category.CategoryResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("1. Category slug uniqueness and collision retry")
    void testCategorySlugUniqueness() {
        CategoryRequest req1 = CategoryRequest.builder()
                .name("Electronics & Gadgets")
                .description("All electronics")
                .build();
        CategoryResponse resp1 = categoryService.createCategory(req1);
        assertThat(resp1.getSlug()).isEqualTo("electronics-gadgets");

        // Duplicate name should generate collision-safe slug
        CategoryRequest req2 = CategoryRequest.builder()
                .name("Electronics & Gadgets")
                .description("Duplicate electronics")
                .build();
        CategoryResponse resp2 = categoryService.createCategory(req2);
        assertThat(resp2.getSlug()).isEqualTo("electronics-gadgets-1");
    }

    @Test
    @DisplayName("2. Category cycle prevention - cannot be its own parent")
    void testCyclePreventionSelfParent() {
        CategoryResponse cat = categoryService.createCategory(CategoryRequest.builder()
                .name("Smartphones")
                .build());

        CategoryRequest updateReq = CategoryRequest.builder()
                .name("Smartphones")
                .parentCategoryId(cat.getId())
                .build();

        assertThatThrownBy(() -> categoryService.updateCategory(cat.getId(), updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A category cannot be its own parent");
    }

    @Test
    @DisplayName("3. Category cycle prevention - cannot have descendant as parent")
    void testCyclePreventionDescendantParent() {
        // Root: Level 1
        CategoryResponse root = categoryService.createCategory(CategoryRequest.builder()
                .name("Level 1 Root")
                .build());

        // Child: Level 2
        CategoryResponse child = categoryService.createCategory(CategoryRequest.builder()
                .name("Level 2 Child")
                .parentCategoryId(root.getId())
                .build());

        // Grandchild: Level 3
        CategoryResponse grandChild = categoryService.createCategory(CategoryRequest.builder()
                .name("Level 3 Grandchild")
                .parentCategoryId(child.getId())
                .build());

        // Attempt to make Level 3 Grandchild the parent of Level 1 Root (creates cycle)
        CategoryRequest cycleReq = CategoryRequest.builder()
                .name("Level 1 Root")
                .parentCategoryId(grandChild.getId())
                .build();

        assertThatThrownBy(() -> categoryService.updateCategory(root.getId(), cycleReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Circular hierarchy detected");
    }

    @Test
    @DisplayName("4. Recursive category tree generation")
    void testCategoryTreeGeneration() {
        CategoryResponse parent = categoryService.createCategory(CategoryRequest.builder()
                .name("Apparel")
                .displayOrder(1)
                .build());

        categoryService.createCategory(CategoryRequest.builder()
                .name("Men's Wear")
                .parentCategoryId(parent.getId())
                .displayOrder(1)
                .build());

        categoryService.createCategory(CategoryRequest.builder()
                .name("Women's Wear")
                .parentCategoryId(parent.getId())
                .displayOrder(2)
                .build());

        List<CategoryResponse> hierarchy = categoryService.getCategoryHierarchy();
        assertThat(hierarchy).isNotEmpty();

        CategoryResponse apparelInTree = hierarchy.stream()
                .filter(c -> c.getName().equals("Apparel"))
                .findFirst()
                .orElse(null);

        assertThat(apparelInTree).isNotNull();
        assertThat(apparelInTree.getSubCategories()).hasSize(2);
        assertThat(apparelInTree.getSubCategories().get(0).getName()).isEqualTo("Men's Wear");
        assertThat(apparelInTree.getSubCategories().get(1).getName()).isEqualTo("Women's Wear");
    }

    @Test
    @DisplayName("5. Category deletion safeguard - cannot delete with subcategories")
    void testDeleteSafeguardSubcategories() {
        CategoryResponse parent = categoryService.createCategory(CategoryRequest.builder()
                .name("Computers")
                .build());

        categoryService.createCategory(CategoryRequest.builder()
                .name("Laptops")
                .parentCategoryId(parent.getId())
                .build());

        assertThatThrownBy(() -> categoryService.deleteCategory(parent.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete category with active subcategories");
    }

    @Test
    @DisplayName("6. Category deletion safeguard - cannot delete with products")
    void testDeleteSafeguardProducts() {
        Category category = categoryRepository.save(Category.builder()
                .name("Books")
                .slug("books-test")
                .active(true)
                .displayOrder(0)
                .build());

        productRepository.save(Product.builder()
                .name("Clean Architecture")
                .slug("clean-architecture-book")
                .sku("BOOK-CLEAN-ARCH")
                .price(BigDecimal.valueOf(49.99))
                .category(category)
                .active(true)
                .build());

        assertThatThrownBy(() -> categoryService.deleteCategory(category.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete category with associated products");
    }

    @Test
    @DisplayName("7. Efficient descendant category ID resolution")
    void testDescendantCategoryIds() {
        CategoryResponse root = categoryService.createCategory(CategoryRequest.builder().name("Root Cat").build());
        CategoryResponse sub1 = categoryService.createCategory(CategoryRequest.builder().name("Sub 1").parentCategoryId(root.getId()).build());
        CategoryResponse sub2 = categoryService.createCategory(CategoryRequest.builder().name("Sub 2").parentCategoryId(sub1.getId()).build());

        Set<Long> descendants = categoryService.getAllDescendantCategoryIds(root.getId());
        assertThat(descendants).contains(root.getId(), sub1.getId(), sub2.getId());
    }
}
