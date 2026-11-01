package com.shopsphere.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventories", indexes = {
        @Index(name = "idx_inventory_product", columnList = "product_id"),
        @Index(name = "idx_inventory_variant", columnList = "variant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", unique = true)
    private ProductVariant variant;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 5;

    /**
     * Net purchasable stock = quantityAvailable - quantityReserved
     */
    public int getPurchasableQuantity() {
        return Math.max(0, (quantityAvailable != null ? quantityAvailable : 0) - (quantityReserved != null ? quantityReserved : 0));
    }

    public boolean hasSufficientStock(int requestedQuantity) {
        return getPurchasableQuantity() >= requestedQuantity;
    }

    public boolean isLowStock() {
        return getPurchasableQuantity() <= lowStockThreshold;
    }
}
