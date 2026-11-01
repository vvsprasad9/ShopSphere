package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsResponse {

    private long totalCustomers;
    private long newCustomersInPeriod;
    private long totalBuyers;
    private long returningBuyersCount;
    private BigDecimal averageOrderValue;
    private Map<String, Long> orderFrequencyDistribution; // "1 order", "2-3 orders", "4+ orders"
}
