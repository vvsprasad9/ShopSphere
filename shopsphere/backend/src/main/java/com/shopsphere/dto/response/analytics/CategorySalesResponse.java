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
public class CategorySalesResponse {

    private Long categoryId;
    private String categoryName;
    private long unitsSold;
    private BigDecimal totalRevenue;
    private BigDecimal percentageOfTotalSales;
}
