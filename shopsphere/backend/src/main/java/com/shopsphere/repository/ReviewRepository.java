package com.shopsphere.repository;

import com.shopsphere.entity.Review;
import com.shopsphere.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double calculateAverageRatingForProduct(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Integer countApprovedReviewsForProduct(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = :status GROUP BY r.rating")
    java.util.List<Object[]> getRatingDistributionForProduct(@Param("productId") Long productId, @Param("status") ReviewStatus status);
}
