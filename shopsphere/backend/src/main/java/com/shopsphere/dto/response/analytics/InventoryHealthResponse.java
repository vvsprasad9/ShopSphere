package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHealthResponse {

    private InventorySummary summary;
    private Page<InventoryItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySummary {
        private long totalUnitsAvailable;
        private long totalUnitsReserved;
        private long totalPurchasableUnits;
        private long lowStockCount;
        private long outOfStockCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemDto {
        private Long inventoryId;
        private Long productId;
        private Long variantId;
        private String itemName;
        private String sku;
        private String categoryName;
        private Integer quantityAvailable;
        private Integer quantityReserved;
        private Integer purchasableQuantity;
        private Integer lowStockThreshold;
        private String stockStatus; // OUT_OF_STOCK, LOW_STOCK, ADEQUATE
    }
}
