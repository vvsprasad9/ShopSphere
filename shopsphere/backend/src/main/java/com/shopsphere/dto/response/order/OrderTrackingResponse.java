package com.shopsphere.dto.response.order;

import com.shopsphere.entity.AddressSnapshot;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {

    private String orderNumber;
    private OrderStatus orderStatus;
    private String trackingNumber;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime orderDate;
    private int totalItems;
    private DeliveryDestination deliveryDestination;
    private List<TrackingItemSummary> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryDestination {
        private String city;
        private String state;
        private String country;

        public static DeliveryDestination fromSnapshot(AddressSnapshot snapshot) {
            if (snapshot == null) return null;
            return DeliveryDestination.builder()
                    .city(snapshot.getCity())
                    .state(snapshot.getState())
                    .country(snapshot.getCountry())
                    .build();
        }
    }

    public static OrderTrackingResponse fromEntity(Order order) {
        if (order == null) return null;

        int totalCount = order.getItems() != null ?
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0;

        List<TrackingItemSummary> itemSummaries = order.getItems() != null ?
                order.getItems().stream()
                        .map(TrackingItemSummary::fromOrderItem)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return OrderTrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .orderDate(order.getCreatedAt())
                .totalItems(totalCount)
                .deliveryDestination(DeliveryDestination.fromSnapshot(order.getShippingAddress()))
                .items(itemSummaries)
                .build();
    }
}
