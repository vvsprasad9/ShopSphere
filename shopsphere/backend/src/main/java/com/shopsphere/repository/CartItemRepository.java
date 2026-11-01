package com.shopsphere.repository;

import com.shopsphere.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(Long cartId, Long productId, Long variantId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantIsNull(Long cartId, Long productId);

    Optional<CartItem> findByIdAndCartUserId(Long itemId, Long userId);

    Optional<CartItem> findByIdAndCartId(Long itemId, Long cartId);

    @Modifying
    void deleteByCartId(Long cartId);
}
