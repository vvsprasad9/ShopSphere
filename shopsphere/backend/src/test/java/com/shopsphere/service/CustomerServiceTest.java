package com.shopsphere.service;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.cart.UpdateCartItemRequest;
import com.shopsphere.dto.request.product.ProductCreateRequest;
import com.shopsphere.dto.request.product.ProductVariantRequest;
import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.dto.request.user.ChangePasswordRequest;
import com.shopsphere.dto.request.user.UpdateProfileRequest;
import com.shopsphere.dto.response.cart.CartItemResponse;
import com.shopsphere.dto.response.cart.CartResponse;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.dto.response.product.ProductVariantResponse;
import com.shopsphere.dto.response.user.AddressResponse;
import com.shopsphere.dto.response.user.UserResponse;
import com.shopsphere.dto.response.wishlist.WishlistResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CartService cartService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customerA;
    private User customerB;
    private Category testCategory;
    private ProductResponse testProduct;
    private ProductVariantResponse testVariant;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));

        customerA = userRepository.save(User.builder()
                .email("alice.customer@shopsphere.com")
                .password(passwordEncoder.encode("AlicePass#123"))
                .firstName("Alice")
                .lastName("Smith")
                .phone("111-222-3333")
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(java.util.List.of(customerRole)))
                .build());

        customerB = userRepository.save(User.builder()
                .email("bob.customer@shopsphere.com")
                .password(passwordEncoder.encode("BobPass#123"))
                .firstName("Bob")
                .lastName("Jones")
                .phone("444-555-6666")
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(java.util.List.of(customerRole)))
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Customer Test Category")
                .slug("customer-test-category")
                .active(true)
                .displayOrder(1)
                .build());

        testProduct = productService.createProduct(ProductCreateRequest.builder()
                .name("Wireless Earbuds ANC")
                .sku("EARBUDS-ANC-01")
                .price(BigDecimal.valueOf(99.99))
                .discountPrice(BigDecimal.valueOf(79.99))
                .categoryId(testCategory.getId())
                .initialStockQuantity(20)
                .build());

        testVariant = productService.addProductVariant(testProduct.getId(), ProductVariantRequest.builder()
                .sku("EARBUDS-ANC-WHITE")
                .variantName("Color: White")
                .priceAdjustment(BigDecimal.valueOf(10.00))
                .stockQuantity(12)
                .build());
    }

    // --- MODULE 1: PROFILE TESTS ---

    @Test
    @DisplayName("1. Retrieve customer profile")
    void testGetProfile() {
        UserResponse profile = userService.getProfile(customerA.getId());
        assertThat(profile.getEmail()).isEqualTo("alice.customer@shopsphere.com");
        assertThat(profile.getFirstName()).isEqualTo("Alice");
        assertThat(profile.getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("2. Update customer profile details")
    void testUpdateProfile() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Alicia")
                .lastName("Smith-Johnson")
                .phone("999-888-7777")
                .build();

        UserResponse updated = userService.updateProfile(customerA.getId(), request);
        assertThat(updated.getFirstName()).isEqualTo("Alicia");
        assertThat(updated.getLastName()).isEqualTo("Smith-Johnson");
        assertThat(updated.getPhone()).isEqualTo("999-888-7777");
    }

    @Test
    @DisplayName("3. Change password successfully and revoke refresh tokens")
    void testChangePasswordSuccess() {
        // Create an active refresh token for Customer A
        refreshTokenRepository.save(RefreshToken.builder()
                .user(customerA)
                .token("active-token-before-pwd-change")
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("AlicePass#123")
                .newPassword("NewAlicePass#456")
                .build();

        userService.changePassword(customerA.getId(), request);

        // Verify new password works
        User refreshed = userRepository.findById(customerA.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewAlicePass#456", refreshed.getPassword())).isTrue();

        // Verify refresh token was revoked
        RefreshToken token = refreshTokenRepository.findByToken("active-token-before-pwd-change").orElseThrow();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("4. Change password with incorrect current password throws BadRequestException")
    void testChangePasswordWrongCurrent() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword#123")
                .newPassword("NewPass#456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(customerA.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password does not match");
    }

    // --- MODULE 2: ADDRESS TESTS ---

    @Test
    @DisplayName("5. First created address automatically becomes default")
    void testFirstAddressBecomesDefault() {
        AddressRequest req = AddressRequest.builder()
                .fullName("Alice Smith")
                .phoneNumber("111-222-3333")
                .addressLine1("100 Market St")
                .city("San Francisco")
                .state("CA")
                .postalCode("94105")
                .country("USA")
                .addressType(AddressType.HOME)
                .isDefault(false) // Requested as false, but first address must become default
                .build();

        AddressResponse res = addressService.createAddress(customerA.getId(), req);
        assertThat(res.isDefault()).isTrue();
    }

    @Test
    @DisplayName("6. Creating second default address unsets previous default")
    void testDefaultAddressSwitching() {
        AddressResponse addr1 = addressService.createAddress(customerA.getId(), AddressRequest.builder()
                .fullName("Alice 1")
                .phoneNumber("111-222-3333")
                .addressLine1("Address 1")
                .city("City 1")
                .state("CA")
                .postalCode("94101")
                .country("USA")
                .build());
        assertThat(addr1.isDefault()).isTrue();

        AddressResponse addr2 = addressService.createAddress(customerA.getId(), AddressRequest.builder()
                .fullName("Alice 2")
                .phoneNumber("111-222-3333")
                .addressLine1("Address 2")
                .city("City 2")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .isDefault(true)
                .build());
        assertThat(addr2.isDefault()).isTrue();

        Address refreshedAddr1 = addressRepository.findById(addr1.getId()).orElseThrow();
        assertThat(refreshedAddr1.isDefault()).isFalse();
    }

    @Test
    @DisplayName("7. Deleting default address promotes remaining address")
    void testDeleteDefaultPromotesFallback() {
        AddressResponse addr1 = addressService.createAddress(customerA.getId(), AddressRequest.builder()
                .fullName("Alice Home")
                .phoneNumber("111-222-3333")
                .addressLine1("Home Address")
                .city("San Francisco")
                .state("CA")
                .postalCode("94101")
                .country("USA")
                .build());

        AddressResponse addr2 = addressService.createAddress(customerA.getId(), AddressRequest.builder()
                .fullName("Alice Office")
                .phoneNumber("111-222-3333")
                .addressLine1("Office Address")
                .city("San Francisco")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .isDefault(false)
                .build());

        // Delete addr1 (which is default)
        addressService.deleteAddress(customerA.getId(), addr1.getId());

        // addr2 must be promoted to default
        Address refreshedAddr2 = addressRepository.findById(addr2.getId()).orElseThrow();
        assertThat(refreshedAddr2.isDefault()).isTrue();
    }

    @Test
    @DisplayName("8. Address ownership validation prevents IDOR")
    void testAddressOwnershipIDOR() {
        AddressResponse addrOfAlice = addressService.createAddress(customerA.getId(), AddressRequest.builder()
                .fullName("Alice Address")
                .phoneNumber("111-222-3333")
                .addressLine1("Alice Street")
                .city("City")
                .state("CA")
                .postalCode("94101")
                .country("USA")
                .build());

        // Bob attempting to view or delete Alice's address throws ResourceNotFoundException
        assertThatThrownBy(() -> addressService.getAddressById(customerB.getId(), addrOfAlice.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> addressService.deleteAddress(customerB.getId(), addrOfAlice.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- MODULE 3: CART TESTS ---

    @Test
    @DisplayName("9. Add product to cart with live server-side pricing")
    void testAddToCartLivePricing() {
        AddToCartRequest req = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build();

        CartResponse cart = cartService.addToCart(customerA.getId(), req);
        assertThat(cart.getItems()).hasSize(1);

        CartItemResponse item = cart.getItems().get(0);
        // Effective price for testProduct is 79.99 (discountPrice)
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(79.99));
        assertThat(item.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(159.98));
        assertThat(cart.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(159.98));
    }

    @Test
    @DisplayName("10. Add variant to cart with independent variant stock")
    void testAddVariantToCart() {
        AddToCartRequest req = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .variantId(testVariant.getId())
                .quantity(3)
                .build();

        CartResponse cart = cartService.addToCart(customerA.getId(), req);
        assertThat(cart.getItems()).hasSize(1);

        CartItemResponse item = cart.getItems().get(0);
        // Base 79.99 + 10.00 adjustment = 89.99
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(89.99));
        assertThat(item.getStockAvailable()).isEqualTo(12); // independent variant stock
    }

    @Test
    @DisplayName("11. Combining duplicate cart items for the same product and variant")
    void testCombineDuplicateCartItems() {
        AddToCartRequest req1 = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build();
        cartService.addToCart(customerA.getId(), req1);

        AddToCartRequest req2 = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(3)
                .build();
        CartResponse cart = cartService.addToCart(customerA.getId(), req2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("12. Exceeding purchasable inventory throws BadRequestException")
    void testExceedingInventoryThrows() {
        AddToCartRequest req = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(25) // Stock is 20
                .build();

        assertThatThrownBy(() -> cartService.addToCart(customerA.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available stock (20)");
    }

    @Test
    @DisplayName("13. Inactive products cannot be added to cart")
    void testInactiveProductCannotBeAdded() {
        Product inactiveProduct = productRepository.save(Product.builder()
                .name("Discontinued Item")
                .slug("discontinued-item")
                .sku("DISC-01")
                .price(BigDecimal.valueOf(50.00))
                .category(testCategory)
                .active(false)
                .build());

        AddToCartRequest req = AddToCartRequest.builder()
                .productId(inactiveProduct.getId())
                .quantity(1)
                .build();

        assertThatThrownBy(() -> cartService.addToCart(customerA.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Product is currently unavailable");
    }

    @Test
    @DisplayName("14. Price update detection when catalog price changes")
    void testPriceUpdateDetection() {
        // Add product at 79.99
        cartService.addToCart(customerA.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        // Admin changes price in catalog to 89.99
        Product product = productRepository.findById(testProduct.getId()).orElseThrow();
        product.setDiscountPrice(BigDecimal.valueOf(89.99));
        productRepository.save(product);

        // Fetching cart should reflect updated price and flag priceChanged
        CartResponse cart = cartService.getCart(customerA.getId());
        CartItemResponse item = cart.getItems().get(0);

        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(89.99));
        assertThat(item.isPriceChanged()).isTrue();
        assertThat(item.getPreviousPrice()).isEqualByComparingTo(BigDecimal.valueOf(79.99));
    }

    @Test
    @DisplayName("15. Remove cart item and clear cart")
    void testRemoveItemAndClearCart() {
        CartResponse cart = cartService.addToCart(customerA.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        Long itemId = cart.getItems().get(0).getId();
        CartResponse afterRemove = cartService.removeCartItem(customerA.getId(), itemId);
        assertThat(afterRemove.getItems()).isEmpty();

        // Add back and clear
        cartService.addToCart(customerA.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        cartService.clearCart(customerA.getId());
        CartResponse emptyCart = cartService.getCart(customerA.getId());
        assertThat(emptyCart.getItems()).isEmpty();
    }

    // --- MODULE 4: WISHLIST TESTS ---

    @Test
    @DisplayName("16. Wishlist add, check, duplicate prevention, and remove")
    void testWishlistLifecycle() {
        // 1. Add to wishlist
        WishlistResponse wishlist = wishlistService.addToWishlist(customerA.getId(), testProduct.getId());
        assertThat(wishlist.getItems()).hasSize(1);
        assertThat(wishlistService.isProductWishlisted(customerA.getId(), testProduct.getId())).isTrue();

        // 2. Duplicate add should not duplicate item
        WishlistResponse duplicateAdd = wishlistService.addToWishlist(customerA.getId(), testProduct.getId());
        assertThat(duplicateAdd.getItems()).hasSize(1);

        // 3. Remove from wishlist
        WishlistResponse afterRemove = wishlistService.removeFromWishlist(customerA.getId(), testProduct.getId());
        assertThat(afterRemove.getItems()).isEmpty();
        assertThat(wishlistService.isProductWishlisted(customerA.getId(), testProduct.getId())).isFalse();
    }
}
