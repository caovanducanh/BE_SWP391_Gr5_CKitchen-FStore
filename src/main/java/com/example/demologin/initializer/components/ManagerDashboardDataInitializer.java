package com.example.demologin.initializer.components;

import com.example.demologin.entity.Batch;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.InventoryDisposal;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.IngredientBatch;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderPriorityConfig;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.ProductionPlan;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardDataInitializer {

        @Value("${app.seed.orders.enabled:false}")
        private boolean seedOrdersEnabled;

    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final InventoryDisposalRepository inventoryDisposalRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderPriorityConfigRepository orderPriorityConfigRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PlanIngredientRepository planIngredientRepository;
    private final PlanIngredientBatchUsageRepository planIngredientBatchUsageRepository;
    
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void initializeManagerDashboardData() {
        log.info("Creating manager dashboard seed data...");

        // === Specific Cleanup for Legacy Init Plans ===
        log.info("🧹 Skipping legacy cleanup to preserve user data.");

        // FORCE UPDATE Schema for MySQL (Hibernate update often fails on column length increases)
        log.info("⚙️ Forcing database schema updates for column lengths...");
        try {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `batches` MODIFY `status` VARCHAR(50)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `production_plans` MODIFY `status` VARCHAR(50)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `ingredient_batches` MODIFY `status` VARCHAR(50)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `orders` MODIFY `id` VARCHAR(30)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `order_items` MODIFY `order_id` VARCHAR(30)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `deliveries` MODIFY `id` VARCHAR(30)").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE `deliveries` MODIFY `order_id` VARCHAR(30)").executeUpdate();
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            
            // Manual cleanup for faulty legacy delivery record
            log.info("🧹 Removing faulty legacy delivery DEL0423001...");
            entityManager.createNativeQuery("DELETE FROM `deliveries` WHERE `id` = 'DEL0423001'").executeUpdate();
            
            log.info("✅ Database schema forced updates and manual cleanup successful.");
        } catch (Exception e) {
            log.warn("⚠️ Forced schema update warning (might already be updated): {}", e.getMessage());
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }

        // Revised General Cleanup: Remove ANYTHING that is not bakery-focused (ID doesn't start with BAKE)
        List<Ingredient> nonBakeryIngredients = ingredientRepository.findAll().stream()
                .filter(i -> !i.getId().startsWith("BAKE"))
                .toList();

        if (!nonBakeryIngredients.isEmpty()) {
            List<String> idsToDelete = nonBakeryIngredients.stream().map(Ingredient::getId).toList();
            log.info("🧹 Optimized General Cleanup: Removing {} non-bakery ingredient entries...", idsToDelete.size());

            // Bulk deletes for high performance
            recipeRepository.deleteByIngredient_IdIn(idsToDelete);
            ingredientBatchRepository.deleteByIngredient_IdIn(idsToDelete);
            kitchenInventoryRepository.deleteByIngredient_IdIn(idsToDelete);

            ingredientRepository.deleteAllInBatch(nonBakeryIngredients);
        }

        ensurePriorityConfigs();
        log.info("🏠 Initializing Kitchens, Staff, and Stores...");
        Kitchen kitchen = ensureKitchen();
        Kitchen kitchen2 = ensureKitchen2();
        ensureKitchenStaffAssignment(kitchen);
        Store store = ensureStore();
        Store store2 = ensureStore2();

        // Update coordinates for existing records if they were null
        updateCoordinatesIfNull();

        log.info("🥞 Finding baseline products...");
        Product product1 = productRepository.findById("PROD001")
                .orElseThrow(() -> new IllegalStateException("Product PROD001 not found"));
        Product product2 = productRepository.findById("PROD002")
                .orElseThrow(() -> new IllegalStateException("Product PROD002 not found"));
        Product product3 = productRepository.findById("PROD003")
                .orElseThrow(() -> new IllegalStateException("Product PROD003 not found"));
        Product product4 = productRepository.findById("PROD004")
                .orElseThrow(() -> new IllegalStateException("Product PROD004 not found"));

        ensureKitchenInventories(kitchen, kitchen2);
        
        log.info("🏪 Ensuring Store Inventories...");
        ensureStoreInventories(store, product1, product2);
        
        log.info("🗑️ Ensuring Inventory Disposals...");
        ensureInventoryDisposals();

        log.info("✅ Manager dashboard seed data ready");
    }

    private void ensurePriorityConfigs() {
        if (orderPriorityConfigRepository.count() > 0) return;

        orderPriorityConfigRepository.saveAll(List.of(
                OrderPriorityConfig.builder().priorityCode("HIGH").minDays(0).maxDays(0).description("Gấp: Giao trong ngày").build(),
                OrderPriorityConfig.builder().priorityCode("NORMAL").minDays(1).maxDays(2).description("Vừa: Giao trong 1-2 ngày").build(),
                OrderPriorityConfig.builder().priorityCode("LOW").minDays(3).maxDays(null).description("Thấp: Giao trên 2 ngày").build()
        ));
        log.info("✅ Order priority configurations initialized");
    }

    private Store ensureStore() {
        return storeRepository.findById("ST001").orElseGet(() -> storeRepository.save(Store.builder()
                .id("ST001")
                .name("Store District 1")
                .address("1 Nguyen Hue, District 1, HCMC")
                .phone("0901000001")
                .manager("manager")
                .status("ACTIVE")
                .openDate(LocalDate.now().minusMonths(3))
                .latitude(10.7769)
                .longitude(106.7037)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private Kitchen ensureKitchen() {
        return kitchenRepository.findById("KIT001").orElseGet(() -> kitchenRepository.save(Kitchen.builder()
                .id("KIT001")
                .name("Central Kitchen HCM")
                .address("99 Tran Hung Dao, District 1, HCMC")
                .phone("0902000002")
                .capacity(500)
                .status("ACTIVE")
                .latitude(10.7686)
                .longitude(106.6953)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

        private Kitchen ensureKitchen2() {
                return kitchenRepository.findById("KIT002").orElseGet(() -> kitchenRepository.save(Kitchen.builder()
                                .id("KIT002")
                                .name("Central Kitchen Thu Duc")
                                .address("25 Vo Van Ngan, Thu Duc, HCMC")
                                .phone("0902000003")
                                .capacity(420)
                                .status("ACTIVE")
                                .latitude(10.8504)
                                .longitude(106.7721)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()));
        }

        private Store ensureStore2() {
                return storeRepository.findById("ST002").orElseGet(() -> storeRepository.save(Store.builder()
                                .id("ST002")
                                .name("Store District 7")
                                .address("88 Nguyen Thi Thap, District 7, HCMC")
                                .phone("0901000002")
                                .manager("manager")
                                .status("ACTIVE")
                                .openDate(LocalDate.now().minusMonths(2))
                                .latitude(10.7412)
                                .longitude(106.7118)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()));
        }

        private void updateCoordinatesIfNull() {
        storeRepository.findById("ST001").ifPresent(s -> {
            if (s.getLatitude() == null) {
                s.setLatitude(10.7769);
                s.setLongitude(106.7037);
                storeRepository.save(s);
            }
        });
        storeRepository.findById("ST002").ifPresent(s -> {
            if (s.getLatitude() == null) {
                s.setLatitude(10.7412);
                s.setLongitude(106.7118);
                storeRepository.save(s);
            }
        });
        kitchenRepository.findById("KIT001").ifPresent(k -> {
            if (k.getLatitude() == null) {
                k.setLatitude(10.7686);
                k.setLongitude(106.6953);
                kitchenRepository.save(k);
            }
        });
        kitchenRepository.findById("KIT002").ifPresent(k -> {
            if (k.getLatitude() == null) {
                k.setLatitude(10.8504);
                k.setLongitude(106.7721);
                kitchenRepository.save(k);
            }
        });
    }

    private void ensureKitchenStaffAssignment(Kitchen kitchen) {
                boolean changed = false;
                List<User> kitchenStaffUsers = userRepository.findAllByRole_Name("CENTRAL_KITCHEN_STAFF");
                for (User staff : kitchenStaffUsers) {
                        if (staff.getKitchen() == null) {
                                staff.setKitchen(kitchen);
                                changed = true;
                        }
                }
                if (changed) {
                        userRepository.saveAll(kitchenStaffUsers);
                        log.info("✅ Assigned kitchen {} to {} central kitchen staff user(s)", kitchen.getId(), kitchenStaffUsers.size());
                }
        }

    private void ensureKitchenInventories(Kitchen kitchen, Kitchen kitchen2) {
        List<Ingredient> allIngredients = ingredientRepository.findAll();
        log.info("Ensuring warehouse inventories for {} ingredients cross kitchens...", allIngredients.size());

        for (Ingredient ing : allIngredients) {
            upsertKitchenInventoryAndBatches(kitchen, ing);
            upsertKitchenInventoryAndBatches(kitchen2, ing);
        }

        log.info("✅ Kitchen inventory and batches initialized.");
    }

    private void upsertKitchenInventoryAndBatches(Kitchen kitchen, Ingredient ingredient) {
        Random random = new Random();
        // Check if we already have a decent amount (at least 1000 units)
        Optional<KitchenInventory> existing = kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id(kitchen.getId(), ingredient.getId());

        // Force update expiry date for existing boost batches to end of year
        List<IngredientBatch> existingBatches = ingredientBatchRepository.findByKitchen_IdAndIngredient_IdAndStatus(kitchen.getId(), ingredient.getId(), "ACTIVE");
        for (IngredientBatch b : existingBatches) {
            if ("SYSTEM_BOOST".equals(b.getSupplier()) || b.getBatchNo().startsWith("BOOST-")) {
                b.setExpiryDate(LocalDate.of(LocalDate.now().getYear(), 12, 31));
                b.setUpdatedAt(LocalDateTime.now());
                ingredientBatchRepository.save(b);
            }
        }

        if (existing.isPresent() && existing.get().getTotalQuantity().compareTo(new BigDecimal("1000")) > 0) {
            return; // Already has plenty
        }

        // Add more batches to boost stock
        double randomQty = 2000.0 + (3000.0 * random.nextDouble());
        BigDecimal qty = BigDecimal.valueOf(randomQty).setScale(2, java.math.RoundingMode.HALF_UP);

        IngredientBatch boostBatch = IngredientBatch.builder()
                .id("B-" + UUID.randomUUID().toString().substring(0, 10))
                .ingredient(ingredient)
                .kitchen(kitchen)
                .batchNo("BOOST-" + (100000 + random.nextInt(900000)))
                .supplier("SYSTEM_BOOST")
                .initialQuantity(qty)
                .remainingQuantity(qty)
                .unit(ingredient.getUnit())
                .expiryDate(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ingredientBatchRepository.save(boostBatch);

        if (existing.isPresent()) {
            KitchenInventory ki = existing.get();
            ki.setTotalQuantity(ki.getTotalQuantity().add(qty));
            ki.setUpdatedAt(LocalDateTime.now());
            kitchenInventoryRepository.save(ki);
        } else {
            KitchenInventory ki = KitchenInventory.builder()
                    .kitchen(kitchen)
                    .ingredient(ingredient)
                    .totalQuantity(qty)
                    .unit(ingredient.getUnit())
                    .minStock(ingredient.getMinStock())
                    .updatedAt(LocalDateTime.now())
                    .build();
            kitchenInventoryRepository.save(ki);
        }
    }

    private void ensureStoreInventories(Store store, Product product1, Product product2) {
        if (storeInventoryRepository.count() > 0) {
            return;
        }

        storeInventoryRepository.saveAll(List.of(
                StoreInventory.builder()
                        .store(store)
                        .product(product1)
                        .quantity(3)
                        .unit("piece")
                        .minStock(8)
                        .updatedAt(LocalDateTime.now())
                        .build(),
                StoreInventory.builder()
                        .store(store)
                        .product(product2)
                        .quantity(20)
                        .unit("piece")
                        .minStock(10)
                        .updatedAt(LocalDateTime.now())
                        .build()
        ));
    }

    private void ensureInventoryDisposals() {
        if (inventoryDisposalRepository.count() > 0) {
            return;
        }

        User disposedBy = userRepository.findByUsername("manager")
                .orElseThrow(() -> new IllegalStateException("Manager user not found"));

        inventoryDisposalRepository.saveAll(List.of(
                InventoryDisposal.builder()
                        .inventoryType("KITCHEN")
                        .itemId(1)
                        .itemName("Bột mì")
                        .quantityDisposed(new BigDecimal("2.5"))
                        .unit("kg")
                        .reason("Expired material")
                        .disposedBy(disposedBy)
                        .disposedAt(LocalDateTime.now().minusDays(1))
                        .build(),
                InventoryDisposal.builder()
                        .inventoryType("STORE")
                        .itemId(1)
                        .itemName("Bánh Mì Sừng Bò")
                        .quantityDisposed(new BigDecimal("1.0"))
                        .unit("piece")
                        .reason("Damaged package")
                        .disposedBy(disposedBy)
                        .disposedAt(LocalDateTime.now().minusHours(10))
                        .build()
        ));
    }
}
