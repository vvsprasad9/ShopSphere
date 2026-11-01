package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendResponse {

    private String periodLabel;
    private long orderCount;
    private BigDecimal grossSales;
    private BigDecimal netSales;
    private BigDecimal discounts;
    private BigDecimal tax;
    private BigDecimal shippingCharges;
    private BigDecimal collectedRevenue;
    private BigDecimal confirmedRefunds;
    private BigDecimal netCollectedRevenue;
    private BigDecimal averageOrderValue;
}
