package com.shopsphere.repository;

import com.shopsphere.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByIdAndActiveTrue(Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    long countByCategoryId(Long categoryId);

    long countByCategoryIdIn(Collection<Long> categoryIds);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.inventory " +
            "WHERE p.slug = :slug")
    Optional<Product> findBySlugWithDetails(@Param("slug") String slug);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.inventory " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    List<Product> findTop8ByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();

    List<Product> findTop8ByCategoryIdAndIdNotAndActiveTrueOrderByCreatedAtDesc(Long categoryId, Long productId);

    List<Product> findTop8ByCategoryIdInAndIdNotAndActiveTrueOrderByCreatedAtDesc(Collection<Long> categoryIds, Long productId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.averageRating = :averageRating, p.reviewCount = :reviewCount WHERE p.id = :productId")
    void updateRatingMetrics(@Param("productId") Long productId, @Param("averageRating") Double averageRating, @Param("reviewCount") Integer reviewCount);
}
