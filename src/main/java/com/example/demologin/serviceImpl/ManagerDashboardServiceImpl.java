package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.KitchenLowStockResponse;
import com.example.demologin.dto.response.ManagerOverviewResponse;
import com.example.demologin.dto.response.StoreLowStockResponse;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.repository.*;
import com.example.demologin.service.ManagerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final InventoryDisposalRepository inventoryDisposalRepository;

    @Override
    public ManagerOverviewResponse getOverview() {
        return ManagerOverviewResponse.builder()
                .totalProducts(productRepository.count())
                .totalIngredients(ingredientRepository.count())
                .totalRecipes(recipeRepository.count())
                .activeProductionPlans(productionPlanRepository.countByStatusIn(List.of("PLANNED", "IN_PROGRESS")))
                .inProgressBatches(batchRepository.countByStatusIn(List.of("IN_PROGRESS")))
                .pendingOrders(orderRepository.countByStatusIn(List.of(
                    OrderStatus.PENDING,
                    OrderStatus.ASSIGNED,
                    OrderStatus.IN_PROGRESS,
                    OrderStatus.PACKED_WAITING_SHIPPER,
                    OrderStatus.SHIPPING,
                    OrderStatus.APPROVED,
                    OrderStatus.PROCESSING
                )))
                .lowStockKitchenItems(kitchenInventoryRepository.countLowStockItems())
                .lowStockStoreItems(storeInventoryRepository.countLowStockItems())
                .totalRevenue(nonNullBigDecimal(salesRecordRepository.sumTotalRevenue()))
                .totalDisposedQuantity(nonNullBigDecimal(inventoryDisposalRepository.sumDisposedQuantity()))
                .build();
    }

    @Override
    public List<KitchenLowStockResponse> getKitchenLowStockItems() {
        return kitchenInventoryRepository.findLowStockItems().stream()
                .map(this::toKitchenLowStock)
                .toList();
    }

    @Override
    public List<StoreLowStockResponse> getStoreLowStockItems() {
        return storeInventoryRepository.findLowStockItems().stream()
                .map(this::toStoreLowStock)
                .toList();
    }

    private KitchenLowStockResponse toKitchenLowStock(KitchenInventory item) {
        return KitchenLowStockResponse.builder()
                .inventoryId(item.getId())
                .ingredientId(item.getIngredient().getId())
                .ingredientName(item.getIngredient().getName())
                .quantity(item.getTotalQuantity())
                .minStock(item.getMinStock())
                .unit(item.getUnit())
                .build();
    }

    private StoreLowStockResponse toStoreLowStock(StoreInventory item) {
        return StoreLowStockResponse.builder()
                .inventoryId(item.getId())
                .storeId(item.getStore().getId())
                .storeName(item.getStore().getName())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .minStock(item.getMinStock())
                .unit(item.getUnit())
                .build();
    }

    private BigDecimal nonNullBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
