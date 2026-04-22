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
import com.example.demologin.repository.BatchRepository;
import com.example.demologin.repository.DeliveryRepository;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.InventoryDisposalRepository;
import com.example.demologin.repository.IngredientBatchRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderPriorityConfigRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
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
import java.util.Random;

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
    private final UserRepository userRepository;

    @Transactional
    public void initializeManagerDashboardData() {
        log.info("Creating manager dashboard seed data...");

        ensurePriorityConfigs();
                Kitchen kitchen = ensureKitchen();
                Kitchen kitchen2 = ensureKitchen2();
                ensureKitchenStaffAssignment(kitchen);
                Store store = ensureStore();
                Store store2 = ensureStore2();

        Product product1 = productRepository.findById("PROD001")
                .orElseThrow(() -> new IllegalStateException("Product PROD001 not found"));
        Product product2 = productRepository.findById("PROD002")
                .orElseThrow(() -> new IllegalStateException("Product PROD002 not found"));

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

        if (seedOrdersEnabled) {
            // ===== ORDERS =====
            Order order1 = ensureOrder(
                    "ORD001", store, kitchen, OrderStatus.PENDING, "HIGH",
                    LocalDateTime.now().minusHours(4), LocalDate.now().plusDays(1),
                    "Cần bổ sung gấp ngượi liệu", "storestaff", new BigDecimal("650000")
            );
            Order order2 = ensureOrder(
                    "ORD002", store, kitchen, OrderStatus.IN_PROGRESS, "NORMAL",
                    LocalDateTime.now().minusHours(8), LocalDate.now().plusDays(2),
                    "Nhập hàng tuần", "storestaff", new BigDecimal("430000")
            );
            Order order3 = ensureOrder(
                    "ORD003", store, kitchen, OrderStatus.ASSIGNED, "NORMAL",
                    LocalDateTime.now().minusDays(1), LocalDate.now().plusDays(3),
                    "Sự kiện cuối tuần", "storestaff", new BigDecimal("520000")
            );
            Order order4 = ensureOrder(
                    "ORD004", store, kitchen, OrderStatus.SHIPPING, "HIGH",
                    LocalDateTime.now().minusDays(2), LocalDate.now(),
                    "Giao hôm nay", "storestaff", new BigDecimal("310000")
            );
            Order order5 = ensureOrder(
                    "ORD005", store, kitchen, OrderStatus.DELIVERED, "NORMAL",
                    LocalDateTime.now().minusDays(3), LocalDate.now().minusDays(1),
                    "Đã nhận 28/03", "storestaff", new BigDecimal("290000")
            );
            Order order6 = ensureOrder(
                    "ORD006", store, kitchen, OrderStatus.CANCELLED, "LOW",
                    LocalDateTime.now().minusDays(4), LocalDate.now().minusDays(2),
                    "Hủy do hết sản phẩm", "storestaff", new BigDecimal("0")
            );
            Order order7 = ensureOrder(
                    "ORD007", store2, null, OrderStatus.PENDING, "HIGH",
                    LocalDateTime.now().minusHours(6), LocalDate.now(),
                    "Đơn mới cần điều phối bếp", "storestaff", new BigDecimal("780000")
            );
            Order order8 = ensureOrder(
                    "ORD008", store2, kitchen2, OrderStatus.ASSIGNED, "NORMAL",
                    LocalDateTime.now().minusHours(10), LocalDate.now().plusDays(1),
                    "Đã phân bếp chi nhánh 2", "storestaff", new BigDecimal("460000")
            );
            Order order9 = ensureOrder(
                    "ORD009", store2, kitchen2, OrderStatus.SHIPPING, "HIGH",
                    LocalDateTime.now().minusDays(1), LocalDate.now(),
                    "Đang giao tuyến Q7", "storestaff", new BigDecimal("510000")
            );
            Order order10 = ensureOrder(
                    "ORD010", store2, kitchen2, OrderStatus.CANCELLED, "NORMAL",
                    LocalDateTime.now().minusDays(2), LocalDate.now().minusDays(1),
                    "Hủy do khách đổi kế hoạch", "storestaff", new BigDecimal("0")
            );
            Order order11 = ensureOrder(
                    "ORD011", store, kitchen, OrderStatus.IN_PROGRESS, "HIGH",
                    LocalDateTime.now().minusDays(2), LocalDate.now().minusDays(1),
                    "Đơn quá hạn cần theo dõi", "storestaff", new BigDecimal("670000")
            );
            Order order12 = ensureOrder(
                    "ORD012", store2, kitchen2, OrderStatus.DELIVERED, "LOW",
                    LocalDateTime.now().minusDays(5), LocalDate.now().minusDays(3),
                    "Đã hoàn tất giao nhận", "storestaff", new BigDecimal("250000")
            );

            // ===== ORDER ITEMS =====
            ensureOrderItem(order1, product1, 50, "piece");
            ensureOrderItem(order1, product2, 30, "piece");
            ensureOrderItem(order2, product1, 40, "piece");
            ensureOrderItem(order2, product2, 20, "piece");
            ensureOrderItem(order3, product1, 60, "piece");
            ensureOrderItem(order3, product2, 25, "piece");
            ensureOrderItem(order4, product2, 35, "piece");
            ensureOrderItem(order5, product1, 30, "piece");
            ensureOrderItem(order5, product2, 15, "piece");
            ensureOrderItem(order6, product1, 10, "piece");
            ensureOrderItem(order7, product1, 45, "piece");
            ensureOrderItem(order7, product2, 18, "piece");
            ensureOrderItem(order8, product1, 28, "piece");
            ensureOrderItem(order8, product2, 22, "piece");
            ensureOrderItem(order9, product1, 35, "piece");
            ensureOrderItem(order10, product2, 16, "piece");
            ensureOrderItem(order11, product1, 42, "piece");
            ensureOrderItem(order11, product2, 27, "piece");
            ensureOrderItem(order12, product2, 14, "piece");

            // ===== DELIVERIES =====
            User coordinator = userRepository.findByUsername("supply")
                    .orElseThrow(() -> new IllegalStateException("Supply coordinator user not found"));

            ensureDelivery("DEL001", order3, coordinator, "ASSIGNED", LocalDateTime.now().minusHours(2));
            ensureDelivery("DEL002", order4, coordinator, "SHIPPING", LocalDateTime.now().minusDays(1));
            ensureDelivery("DEL003", order5, coordinator, "DELIVERED", LocalDateTime.now().minusDays(2));
            ensureDelivery("DEL004", order8, coordinator, "ASSIGNED", LocalDateTime.now().minusHours(5));
            ensureDelivery("DEL005", order9, coordinator, "DELAYED", LocalDateTime.now().minusHours(20));
            ensureDelivery("DEL006", order10, coordinator, "CANCELLED", LocalDateTime.now().minusHours(30));
            ensureDelivery("DEL007", order12, coordinator, "DELIVERED", LocalDateTime.now().minusDays(4));

            ensureBatch(
                    "BATCH001", plan1, product1, kitchen,
                    80, 80, "cai", "AVAILABLE", LocalDate.now().plusDays(5),
                    "kitchen"
            );
        } else {
            log.info("Skipping order/delivery seed data (app.seed.orders.enabled=false)");
        }

        ensureKitchenInventories(kitchen, kitchen2);
        ensureStoreInventories(store, product1, product2);
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

        private Kitchen ensureKitchen2() {
                return kitchenRepository.findById("KIT002").orElseGet(() -> kitchenRepository.save(Kitchen.builder()
                                .id("KIT002")
                                .name("Central Kitchen Thu Duc")
                                .address("25 Vo Van Ngan, Thu Duc, HCMC")
                                .phone("0902000003")
                                .capacity(420)
                                .status("ACTIVE")
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
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()));
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
                              OrderStatus status,
                              String priority,
                              LocalDateTime createdAt,
                              LocalDate requestedDate,
                              String notes,
                              String createdBy,
                              BigDecimal total) {
        return orderRepository.findById(id)
                .map(existing -> {
                    existing.setStore(store);
                    existing.setKitchen(kitchen);
                    existing.setStatus(status);
                    existing.setPriority(priority);
                    existing.setCreatedAt(createdAt);
                    existing.setRequestedDate(requestedDate);
                    existing.setNotes(notes);
                    existing.setCreatedBy(createdBy);
                    existing.setTotal(total);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(existing);
                })
                .orElseGet(() -> orderRepository.save(Order.builder()
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

    private void ensureDelivery(String id, Order order, User coordinator, String status, LocalDateTime assignedAt) {
        deliveryRepository.findById(id)
                .map(existing -> {
                    existing.setOrder(order);
                    existing.setCoordinator(coordinator);
                    existing.setStatus(status);
                    existing.setAssignedAt(assignedAt);
                    existing.setDeliveredAt("DELIVERED".equalsIgnoreCase(status) ? assignedAt.plusHours(4) : null);
                    existing.setNotes(buildDeliveryNote(status));
                    existing.setReceiverName("DELIVERED".equalsIgnoreCase(status) ? "Store Receiver" : null);
                    existing.setTemperatureOk("DELIVERED".equalsIgnoreCase(status) ? Boolean.TRUE : null);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return deliveryRepository.save(existing);
                })
                .orElseGet(() -> deliveryRepository.save(Delivery.builder()
                        .id(id)
                        .order(order)
                        .coordinator(coordinator)
                        .status(status)
                        .assignedAt(assignedAt)
                        .deliveredAt("DELIVERED".equalsIgnoreCase(status) ? assignedAt.plusHours(4) : null)
                        .notes(buildDeliveryNote(status))
                        .receiverName("DELIVERED".equalsIgnoreCase(status) ? "Store Receiver" : null)
                        .temperatureOk("DELIVERED".equalsIgnoreCase(status) ? Boolean.TRUE : null)
                        .createdAt(assignedAt.minusMinutes(30))
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

        private String buildDeliveryNote(String status) {
                if ("DELAYED".equalsIgnoreCase(status)) {
                        return "Trễ chuyến do kẹt xe giờ cao điểm";
                }
                if ("CANCELLED".equalsIgnoreCase(status)) {
                        return "Hủy chuyến do cửa hàng thay đổi nhu cầu";
                }
                return "";
        }

    private void ensureBatch(String id,
                             ProductionPlan plan,
                             Product product,
                             Kitchen kitchen,
                             Integer quantity,
                             Integer remainingQuantity,
                             String unit,
                             String status,
                             LocalDate expiryDate,
                             String staff) {
        batchRepository.findById(id).orElseGet(() -> batchRepository.save(Batch.builder()
                .id(id)
                .plan(plan)
                .product(product)
                .kitchen(kitchen)
                .quantity(quantity)
                .remainingQuantity(remainingQuantity)
                .unit(unit)
                .status(status)
                .expiryDate(expiryDate)
                .staff(staff)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private void ensureOrderItem(Order order, Product product, int quantity, String unit) {
        boolean exists = orderItemRepository.findByOrder_Id(order.getId())
                .stream().anyMatch(i -> i.getProduct().getId().equals(product.getId()));
        if (exists) return;
        orderItemRepository.save(
                com.example.demologin.entity.OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(quantity)
                        .unit(unit)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private void ensureKitchenInventories(Kitchen kitchen, Kitchen kitchen2) {
        List<Ingredient> allIngredients = ingredientRepository.findAll();
        log.info("Ensuring warehouse inventories for {} ingredients cross kitchens...", allIngredients.size());

        Random random = new Random();

        for (Ingredient ing : allIngredients) {
            if (!kitchenInventoryRepository.existsByKitchen_IdAndIngredient_Id(kitchen.getId(), ing.getId())) {
                createKitchenInventoryAndBatches(kitchen, ing, random);
            }
            if (!kitchenInventoryRepository.existsByKitchen_IdAndIngredient_Id(kitchen2.getId(), ing.getId())) {
                createKitchenInventoryAndBatches(kitchen2, ing, random);
            }
        }

        log.info("✅ Kitchen inventory and batches initialized.");
    }

    private void createKitchenInventoryAndBatches(Kitchen kitchen, Ingredient ingredient, Random random) {
        double randomQty1 = 10.0 + (50.0 * random.nextDouble());
        double randomQty2 = 10.0 + (50.0 * random.nextDouble());
        BigDecimal qty1 = BigDecimal.valueOf(randomQty1).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal qty2 = BigDecimal.valueOf(randomQty2).setScale(2, java.math.RoundingMode.HALF_UP);

        IngredientBatch batch1 = IngredientBatch.builder()
                .ingredient(ingredient)
                .kitchen(kitchen)
                .batchNo("BATCH-" + (100000 + random.nextInt(900000)))
                .supplier(ingredient.getSupplier())
                .initialQuantity(qty1)
                .remainingQuantity(qty1)
                .unit(ingredient.getUnit())
                .expiryDate(LocalDate.now().plusDays(random.nextInt(30) + 10))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        IngredientBatch batch2 = IngredientBatch.builder()
                .ingredient(ingredient)
                .kitchen(kitchen)
                .batchNo("BATCH-" + (100000 + random.nextInt(900000)))
                .supplier(ingredient.getSupplier())
                .initialQuantity(qty2)
                .remainingQuantity(qty2)
                .unit(ingredient.getUnit())
                .expiryDate(LocalDate.now().plusDays(random.nextInt(30) + 40))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        ingredientBatchRepository.saveAll(List.of(batch1, batch2));

        KitchenInventory ki = KitchenInventory.builder()
                .kitchen(kitchen)
                .ingredient(ingredient)
                .totalQuantity(qty1.add(qty2))
                .unit(ingredient.getUnit())
                .minStock(ingredient.getMinStock())
                .updatedAt(LocalDateTime.now())
                .build();
        kitchenInventoryRepository.save(ki);
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
