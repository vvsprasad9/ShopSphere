package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import com.shopsphere.entity.Inventory;
import com.shopsphere.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryAuthoritativeStockTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Should verify Inventory as the authoritative source of stock and calculate available quantity correctly")
    void testInventoryAuthoritativeStockSource() {
        Category category = categoryRepository.save(Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Smart gadgets and electronics")
                .build());

        Product product = Product.builder()
                .name("Wireless Noise-Cancelling Headphones")
                .slug("wireless-nc-headphones")
                .sku("SKU-WNC-001")
                .price(new BigDecimal("199.99"))
                .discountPrice(new BigDecimal("179.99"))
                .brand("AcousticPro")
                .category(category)
                .active(true)
                .featured(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // Assign authoritative Inventory
        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantityAvailable(50)
                .quantityReserved(10)
                .lowStockThreshold(5)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        savedProduct.setInventory(savedInventory);

        assertThat(savedInventory.getId()).isNotNull();
        // 50 available - 10 reserved = 40 purchasable
        assertThat(savedInventory.getPurchasableQuantity()).isEqualTo(40);
        assertThat(savedInventory.hasSufficientStock(40)).isTrue();
        assertThat(savedInventory.hasSufficientStock(41)).isFalse();
        assertThat(savedInventory.isLowStock()).isFalse();

        // Verify retrieval by product
        Optional<Inventory> retrieved = inventoryRepository.findByProductId(savedProduct.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getQuantityAvailable()).isEqualTo(50);
        assertThat(retrieved.get().getQuantityReserved()).isEqualTo(10);
    }
}
