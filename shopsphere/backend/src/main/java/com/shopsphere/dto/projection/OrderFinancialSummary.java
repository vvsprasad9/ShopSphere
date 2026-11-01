package com.shopsphere.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderFinancialSummary {

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private long orderCount;

    public OrderFinancialSummary(Number subtotal, Number discountAmount, Number shippingFee, Number taxAmount, Number totalAmount, Number orderCount) {
        this.subtotal = subtotal != null ? new BigDecimal(subtotal.toString()) : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? new BigDecimal(discountAmount.toString()) : BigDecimal.ZERO;
        this.shippingFee = shippingFee != null ? new BigDecimal(shippingFee.toString()) : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? new BigDecimal(taxAmount.toString()) : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? new BigDecimal(totalAmount.toString()) : BigDecimal.ZERO;
        this.orderCount = orderCount != null ? orderCount.longValue() : 0L;
    }
}
