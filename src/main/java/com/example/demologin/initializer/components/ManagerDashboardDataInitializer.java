package com.example.demologin.initializer.components;

import com.example.demologin.entity.Batch;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.InventoryDisposal;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.ProductionPlan;
import com.example.demologin.entity.SalesRecord;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.entity.User;
import com.example.demologin.repository.BatchRepository;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.InventoryDisposalRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
import com.example.demologin.repository.SalesRecordRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardDataInitializer {

    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final InventoryDisposalRepository inventoryDisposalRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    @Transactional
    public void initializeManagerDashboardData() {
        log.info("Creating manager dashboard seed data...");

        Store store = ensureStore();
        Kitchen kitchen = ensureKitchen();

        Product product1 = productRepository.findById("PROD001")
                .orElseThrow(() -> new IllegalStateException("Product PROD001 not found"));
        Product product2 = productRepository.findById("PROD002")
                .orElseThrow(() -> new IllegalStateException("Product PROD002 not found"));

        Ingredient ingredient1 = ingredientRepository.findById("ING001")
                .orElseThrow(() -> new IllegalStateException("Ingredient ING001 not found"));
        Ingredient ingredient2 = ingredientRepository.findById("ING002")
                .orElseThrow(() -> new IllegalStateException("Ingredient ING002 not found"));

        ProductionPlan plan1 = ensureProductionPlan(
                "PLAN001", product1, 120, "cai", "PLANNED",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                "kitchen", "Morning production wave"
        );
        ensureProductionPlan(
                "PLAN002", product2, 180, "o", "IN_PROGRESS",
                LocalDateTime.now().minusHours(6), LocalDateTime.now().plusDays(1),
                "kitchen", "Afternoon production wave"
        );

        Order order1 = ensureOrder(
                "ORD001", store, kitchen, "PENDING", "HIGH",
                LocalDateTime.now().minusHours(4), LocalDate.now().plusDays(1),
                "Need urgent refill", "manager", new BigDecimal("650000")
        );
        ensureOrder(
                "ORD002", store, kitchen, "PROCESSING", "NORMAL",
                LocalDateTime.now().minusHours(2), LocalDate.now().plusDays(2),
                "Weekly restock", "manager", new BigDecimal("430000")
        );
        ensureOrder(
                "ORD003", store, kitchen, "APPROVED", "NORMAL",
                LocalDateTime.now().minusHours(1), LocalDate.now().plusDays(3),
                "Store event restock", "manager", new BigDecimal("520000")
        );

        ensureBatch(
                "BATCH001", order1, 1, plan1, product1, kitchen,
                80, "cai", "IN_PROGRESS", LocalDateTime.now().minusHours(1), null,
                "kitchen"
        );

        ensureKitchenInventories(ingredient1, ingredient2);
        ensureStoreInventories(store, product1, product2);

        ensureSalesRecords(store);
        ensureInventoryDisposals();

        log.info("✅ Manager dashboard seed data ready");
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private ProductionPlan ensureProductionPlan(String id,
                                                Product product,
                                                Integer quantity,
                                                String unit,
                                                String status,
                                                LocalDateTime startDate,
                                                LocalDateTime endDate,
                                                String staff,
                                                String notes) {
        return productionPlanRepository.findById(id).orElseGet(() -> productionPlanRepository.save(ProductionPlan.builder()
                .id(id)
                .product(product)
                .quantity(quantity)
                .unit(unit)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .staff(staff)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private Order ensureOrder(String id,
                              Store store,
                              Kitchen kitchen,
                              String status,
                              String priority,
                              LocalDateTime createdAt,
                              LocalDate requestedDate,
                              String notes,
                              String createdBy,
                              BigDecimal total) {
        return orderRepository.findById(id).orElseGet(() -> orderRepository.save(Order.builder()
                .id(id)
                .store(store)
                .kitchen(kitchen)
                .status(status)
                .priority(priority)
                .createdAt(createdAt)
                .requestedDate(requestedDate)
                .notes(notes)
                .createdBy(createdBy)
                .total(total)
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private void ensureBatch(String id,
                             Order order,
                             Integer orderItemIndex,
                             ProductionPlan plan,
                             Product product,
                             Kitchen kitchen,
                             Integer quantity,
                             String unit,
                             String status,
                             LocalDateTime startDate,
                             LocalDateTime endDate,
                             String staff) {
        batchRepository.findById(id).orElseGet(() -> batchRepository.save(Batch.builder()
                .id(id)
                .order(order)
                .orderItemIndex(orderItemIndex)
                .plan(plan)
                .product(product)
                .kitchen(kitchen)
                .quantity(quantity)
                .unit(unit)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .staff(staff)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private void ensureKitchenInventories(Ingredient ingredient1, Ingredient ingredient2) {
        if (kitchenInventoryRepository.count() > 0) {
            return;
        }

        kitchenInventoryRepository.saveAll(List.of(
                KitchenInventory.builder()
                        .ingredient(ingredient1)
                        .quantity(new BigDecimal("4"))
                        .unit("kg")
                        .minStock(10)
                        .batchNo("KINV-001")
                        .supplier("Cong ty Bot Mi")
                        .updatedAt(LocalDateTime.now())
                        .build(),
                KitchenInventory.builder()
                        .ingredient(ingredient2)
                        .quantity(new BigDecimal("12"))
                        .unit("kg")
                        .minStock(5)
                        .batchNo("KINV-002")
                        .supplier("Cong ty Duong")
                        .updatedAt(LocalDateTime.now())
                        .build()
        ));
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

    private void ensureSalesRecords(Store store) {
        salesRecordRepository.findById("SR001").orElseGet(() -> salesRecordRepository.save(SalesRecord.builder()
                .id("SR001")
                .store(store)
                .date(LocalDate.now().minusDays(1))
                .totalRevenue(new BigDecimal("3200000"))
                .recordedBy("manager")
                .recordedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build()));

        salesRecordRepository.findById("SR002").orElseGet(() -> salesRecordRepository.save(SalesRecord.builder()
                .id("SR002")
                .store(store)
                .date(LocalDate.now())
                .totalRevenue(new BigDecimal("2800000"))
                .recordedBy("manager")
                .recordedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build()));
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
