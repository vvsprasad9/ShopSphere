package com.shopsphere.repository.specification;

import com.shopsphere.dto.request.product.ProductSearchCriteria;
import com.shopsphere.entity.Inventory;
import com.shopsphere.entity.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Ensure distinct results when joins occur
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            // Active flag
            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            // Featured flag
            if (criteria.getFeatured() != null) {
                predicates.add(cb.equal(root.get("featured"), criteria.getFeatured()));
            }

            // Keyword match across name, description, brand
            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String pattern = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate brandMatch = cb.like(cb.lower(root.get("brand")), pattern);
                predicates.add(cb.or(nameMatch, descMatch, brandMatch));
            }

            // Brand filter
            if (criteria.getBrand() != null && !criteria.getBrand().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), criteria.getBrand().trim().toLowerCase()));
            }

            // Category filter including descendant subcategories
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(criteria.getCategoryIds()));
            } else if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
            }

            // Effective price handling (if discountPrice > 0, use discountPrice, else price)
            Expression<BigDecimal> effectivePrice = cb.selectCase()
                    .when(cb.and(
                            cb.isNotNull(root.get("discountPrice")),
                            cb.greaterThan(root.get("discountPrice"), BigDecimal.ZERO)
                    ), root.get("discountPrice"))
                    .otherwise(root.get("price"))
                    .as(BigDecimal.class);

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectivePrice, criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(effectivePrice, criteria.getMaxPrice()));
            }

            // Minimum Average Rating
            if (criteria.getMinRating() != null && criteria.getMinRating() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), criteria.getMinRating()));
            }

            // In-stock availability: (quantityAvailable - quantityReserved) > 0
            if (Boolean.TRUE.equals(criteria.getInStockOnly())) {
                Join<Product, Inventory> inventoryJoin = root.join("inventory", JoinType.LEFT);
                Expression<Integer> purchasableStock = cb.diff(
                        cb.coalesce(inventoryJoin.get("quantityAvailable"), 0),
                        cb.coalesce(inventoryJoin.get("quantityReserved"), 0)
                );
                predicates.add(cb.greaterThan(purchasableStock, 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
