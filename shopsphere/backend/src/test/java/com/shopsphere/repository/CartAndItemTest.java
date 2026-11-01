package com.shopsphere.repository;

import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.UserStatus;
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
class CartAndItemTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("Should persist Cart and CartItem with price snapshots and compute subtotal correctly")
    void testCartItemCalculationsAndRelationships() {
        User user = userRepository.save(User.builder()
                .email("shopper@shopsphere.com")
                .password("hash")
                .firstName("Sarah")
                .lastName("Connor")
                .status(UserStatus.ACTIVE)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Apparel")
                .slug("apparel")
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Classic Cotton T-Shirt")
                .slug("classic-cotton-tshirt")
                .sku("SKU-TSHIRT-001")
                .price(new BigDecimal("25.00"))
                .brand("ShopSphere Basics")
                .category(category)
                .build());

        Cart cart = cartRepository.save(Cart.builder()
                .user(user)
                .build());

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(3)
                .unitPriceSnapshot(new BigDecimal("25.00"))
                .build();

        cart.getItems().add(item);
        cartItemRepository.save(item);

        assertThat(item.calculateTotalPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(cart.calculateSubtotal()).isEqualByComparingTo(new BigDecimal("75.00"));

        Optional<Cart> fetchedCart = cartRepository.findByUserIdWithItems(user.getId());
        assertThat(fetchedCart).isPresent();
        assertThat(fetchedCart.get().getItems()).hasSize(1);
        assertThat(fetchedCart.get().getItems().get(0).getProduct().getName()).isEqualTo("Classic Cotton T-Shirt");
    }
}
