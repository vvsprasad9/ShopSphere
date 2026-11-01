package com.shopsphere.repository;

import com.shopsphere.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    @Modifying
    void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
}
