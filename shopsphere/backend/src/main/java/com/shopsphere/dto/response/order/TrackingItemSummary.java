package com.shopsphere.dto.response.order;

import com.shopsphere.entity.OrderItem;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingItemSummary {

    private String productName;
    private Integer quantity;

    public static TrackingItemSummary fromOrderItem(OrderItem item) {
        if (item == null) return null;
        return TrackingItemSummary.builder()
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .build();
    }
}
