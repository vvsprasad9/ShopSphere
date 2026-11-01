package com.shopsphere.service.impl;

import com.shopsphere.entity.Inventory;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductVariant;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.InventoryRepository;
import com.shopsphere.service.InventoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void reserveStock(Product product, ProductVariant variant, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Reservation quantity must be positive");
        }

        Inventory inventory = getInventoryWithLock(product, variant);
        int available = inventory.getQuantityAvailable() != null ? inventory.getQuantityAvailable() : 0;
        int reserved = inventory.getQuantityReserved() != null ? inventory.getQuantityReserved() : 0;
        int purchasable = Math.max(0, available - reserved);

        String itemName = variant != null ? (product.getName() + " (" + variant.getSku() + ")") : product.getName();

        if (purchasable < quantity) {
            log.warn("Insufficient stock for item {}. Purchasable: {}, Requested: {}", itemName, purchasable, quantity);
            throw new BadRequestException("Insufficient stock for item: " + itemName + ". Available: " + purchasable);
        }

        inventory.setQuantityReserved(reserved + quantity);
        inventoryRepository.save(inventory);
        log.info("Reserved {} units for {}. New reserved: {}", quantity, itemName, inventory.getQuantityReserved());
    }

    @Override
    @Transactional
    public void commitReservedStock(Product product, ProductVariant variant, int quantity) {
        if (quantity <= 0) return;

        Inventory inventory = getInventoryWithLock(product, variant);
        int available = inventory.getQuantityAvailable() != null ? inventory.getQuantityAvailable() : 0;
        int reserved = inventory.getQuantityReserved() != null ? inventory.getQuantityReserved() : 0;

        int newReserved = Math.max(0, reserved - quantity);
        int newAvailable = Math.max(0, available - quantity);

        inventory.setQuantityReserved(newReserved);
        inventory.setQuantityAvailable(newAvailable);
        inventoryRepository.save(inventory);

        String itemName = variant != null ? (product.getName() + " (" + variant.getSku() + ")") : product.getName();
        log.info("Committed {} units for {}. Remaining available: {}, reserved: {}", quantity, itemName, newAvailable, newReserved);
    }

    @Override
    @Transactional
    public void releaseReservedStock(Product product, ProductVariant variant, int quantity) {
        if (quantity <= 0) return;

        Inventory inventory = getInventoryWithLock(product, variant);
        int reserved = inventory.getQuantityReserved() != null ? inventory.getQuantityReserved() : 0;
        int newReserved = Math.max(0, reserved - quantity);

        inventory.setQuantityReserved(newReserved);
        inventoryRepository.save(inventory);

        String itemName = variant != null ? (product.getName() + " (" + variant.getSku() + ")") : product.getName();
        log.info("Released {} reserved units for {}. New reserved: {}", quantity, itemName, newReserved);
    }

    @Override
    @Transactional
    public void restoreCommittedStock(Product product, ProductVariant variant, int quantity) {
        if (quantity <= 0) return;

        Inventory inventory = getInventoryWithLock(product, variant);
        int available = inventory.getQuantityAvailable() != null ? inventory.getQuantityAvailable() : 0;
        inventory.setQuantityAvailable(available + quantity);
        inventoryRepository.save(inventory);

        String itemName = variant != null ? (product.getName() + " (" + variant.getSku() + ")") : product.getName();
        log.info("Restored {} units of stock for {}. New available: {}", quantity, itemName, inventory.getQuantityAvailable());
    }

    @Override
    @Transactional(readOnly = true)
    public int getPurchasableStock(Product product, ProductVariant variant) {
        Optional<Inventory> invOpt = getInventoryWithoutLock(product, variant);
        return invOpt.map(Inventory::getPurchasableQuantity).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientStock(Product product, ProductVariant variant, int quantity) {
        return getPurchasableStock(product, variant) >= quantity;
    }

    private Inventory getInventoryWithLock(Product product, ProductVariant variant) {
        Long inventoryId;
        if (variant != null) {
            inventoryId = inventoryRepository.findByVariantId(variant.getId())
                    .map(Inventory::getId)
                    .orElseThrow(() -> new BadRequestException("Inventory record not found for variant id: " + variant.getId()));
        } else if (product != null) {
            inventoryId = inventoryRepository.findByProductId(product.getId())
                    .map(Inventory::getId)
                    .orElseThrow(() -> new BadRequestException("Inventory record not found for product id: " + product.getId()));
        } else {
            throw new BadRequestException("Product and variant cannot both be null for inventory operation");
        }

        Inventory managed = entityManager.find(Inventory.class, inventoryId);
        if (managed != null) {
            entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
            return managed;
        } else {
            return entityManager.find(Inventory.class, inventoryId, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    private Optional<Inventory> getInventoryWithoutLock(Product product, ProductVariant variant) {
        if (variant != null) {
            return inventoryRepository.findByVariantId(variant.getId());
        } else if (product != null) {
            return inventoryRepository.findByProductId(product.getId());
        }
        return Optional.empty();
    }
}
