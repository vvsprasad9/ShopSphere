package com.shopsphere.service;

import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductVariant;

public interface InventoryService {

    void reserveStock(Product product, ProductVariant variant, int quantity);

    void commitReservedStock(Product product, ProductVariant variant, int quantity);

    void releaseReservedStock(Product product, ProductVariant variant, int quantity);

    void restoreCommittedStock(Product product, ProductVariant variant, int quantity);

    int getPurchasableStock(Product product, ProductVariant variant);

    boolean hasSufficientStock(Product product, ProductVariant variant, int quantity);
}
