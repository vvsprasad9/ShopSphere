package com.shopsphere.repository;

import com.shopsphere.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    Optional<Inventory> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithPessimisticLock(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdWithPessimisticLock(@Param("variantId") Long variantId);

    @Query("SELECT " +
            "COALESCE(SUM(i.quantityAvailable), 0), " +
            "COALESCE(SUM(i.quantityReserved), 0), " +
            "COALESCE(SUM(CASE WHEN (i.quantityAvailable - i.quantityReserved) <= 0 THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN (i.quantityAvailable - i.quantityReserved) > 0 AND (i.quantityAvailable - i.quantityReserved) <= i.lowStockThreshold THEN 1 ELSE 0 END), 0) " +
            "FROM Inventory i " +
            "WHERE (i.variant IS NOT NULL) " +
            "   OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))")
    java.util.List<Object[]> getDeduplicatedInventorySummary();

    @Query(value = "SELECT i FROM Inventory i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH i.variant v " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE (i.variant IS NOT NULL) " +
            "   OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))",
            countQuery = "SELECT COUNT(i) FROM Inventory i " +
            "WHERE (i.variant IS NOT NULL) " +
            "   OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))")
    org.springframework.data.domain.Page<Inventory> findDeduplicatedInventories(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT i FROM Inventory i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH i.variant v " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE ((i.variant IS NOT NULL) OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))) " +
            "  AND (i.quantityAvailable - i.quantityReserved) <= 0",
            countQuery = "SELECT COUNT(i) FROM Inventory i " +
            "WHERE ((i.variant IS NOT NULL) OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))) " +
            "  AND (i.quantityAvailable - i.quantityReserved) <= 0")
    org.springframework.data.domain.Page<Inventory> findDeduplicatedOutOfStockInventories(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT i FROM Inventory i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH i.variant v " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE ((i.variant IS NOT NULL) OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))) " +
            "  AND (i.quantityAvailable - i.quantityReserved) > 0 " +
            "  AND (i.quantityAvailable - i.quantityReserved) <= i.lowStockThreshold",
            countQuery = "SELECT COUNT(i) FROM Inventory i " +
            "WHERE ((i.variant IS NOT NULL) OR (i.product IS NOT NULL AND i.variant IS NULL AND NOT EXISTS (SELECT 1 FROM ProductVariant pv WHERE pv.product = i.product))) " +
            "  AND (i.quantityAvailable - i.quantityReserved) > 0 " +
            "  AND (i.quantityAvailable - i.quantityReserved) <= i.lowStockThreshold")
    org.springframework.data.domain.Page<Inventory> findDeduplicatedLowStockInventories(org.springframework.data.domain.Pageable pageable);
}
