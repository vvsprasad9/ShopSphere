package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAnalyticsResponse {

    private CashCollectionBreakdown cashBreakdown;
    private List<PaymentMethodMetric> methodDistribution;
    private List<PaymentStatusMetric> statusDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashCollectionBreakdown {
        private BigDecimal collectedRevenue;
        private BigDecimal pendingCollections;
        private BigDecimal pendingCodAmount;
        private BigDecimal pendingOnlineAmount;
        private BigDecimal confirmedRefundAmount;
        private BigDecimal netCollectedRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodMetric {
        private String paymentMethod;
        private long transactionCount;
        private BigDecimal totalAmount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatusMetric {
        private String paymentStatus;
        private long transactionCount;
        private BigDecimal totalAmount;
    }
}
