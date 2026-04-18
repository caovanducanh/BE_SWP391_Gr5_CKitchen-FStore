package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.KitchenLowStockResponse;
import com.example.demologin.dto.response.ManagerOverviewResponse;
import com.example.demologin.dto.response.StoreLowStockResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.repository.BatchRepository;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.InventoryDisposalRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
import com.example.demologin.repository.RecipeRepository;
import com.example.demologin.repository.SalesRecordRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private ProductionPlanRepository productionPlanRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock
    private StoreInventoryRepository storeInventoryRepository;
    @Mock
    private SalesRecordRepository salesRecordRepository;
    @Mock
    private InventoryDisposalRepository inventoryDisposalRepository;

    @InjectMocks
    private ManagerDashboardServiceImpl managerDashboardService;

    @Test
    void getOverview_shouldAggregateAllManagerMetrics() {
        when(productRepository.count()).thenReturn(5L);
        when(ingredientRepository.count()).thenReturn(7L);
        when(recipeRepository.count()).thenReturn(9L);
        when(productionPlanRepository.countByStatusIn(List.of("PLANNED", "IN_PROGRESS"))).thenReturn(2L);
        when(batchRepository.countByStatusIn(List.of("IN_PROGRESS"))).thenReturn(3L);
        when(orderRepository.countByStatusIn(List.of(
            OrderStatus.PENDING,
            OrderStatus.ASSIGNED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.PACKED_WAITING_SHIPPER,
            OrderStatus.SHIPPING,
            OrderStatus.APPROVED,
            OrderStatus.PROCESSING
        ))).thenReturn(4L);
        when(kitchenInventoryRepository.countLowStockItems()).thenReturn(1L);
        when(storeInventoryRepository.countLowStockItems()).thenReturn(6L);
        when(salesRecordRepository.sumTotalRevenue()).thenReturn(BigDecimal.valueOf(123.45));
        when(inventoryDisposalRepository.sumDisposedQuantity()).thenReturn(BigDecimal.valueOf(8.9));

        ManagerOverviewResponse result = managerDashboardService.getOverview();

        assertEquals(5L, result.getTotalProducts());
        assertEquals(7L, result.getTotalIngredients());
        assertEquals(9L, result.getTotalRecipes());
        assertEquals(2L, result.getActiveProductionPlans());
        assertEquals(3L, result.getInProgressBatches());
        assertEquals(4L, result.getPendingOrders());
        assertEquals(1L, result.getLowStockKitchenItems());
        assertEquals(6L, result.getLowStockStoreItems());
        assertEquals(BigDecimal.valueOf(123.45), result.getTotalRevenue());
        assertEquals(BigDecimal.valueOf(8.9), result.getTotalDisposedQuantity());
    }

    @Test
    void getOverview_shouldUseZeroWhenRevenueAndDisposalAreNull() {
        when(productRepository.count()).thenReturn(0L);
        when(ingredientRepository.count()).thenReturn(0L);
        when(recipeRepository.count()).thenReturn(0L);
        when(productionPlanRepository.countByStatusIn(List.of("PLANNED", "IN_PROGRESS"))).thenReturn(0L);
        when(batchRepository.countByStatusIn(List.of("IN_PROGRESS"))).thenReturn(0L);
        when(orderRepository.countByStatusIn(List.of(
            OrderStatus.PENDING,
            OrderStatus.ASSIGNED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.PACKED_WAITING_SHIPPER,
            OrderStatus.SHIPPING,
            OrderStatus.APPROVED,
            OrderStatus.PROCESSING
        ))).thenReturn(0L);
        when(kitchenInventoryRepository.countLowStockItems()).thenReturn(0L);
        when(storeInventoryRepository.countLowStockItems()).thenReturn(0L);
        when(salesRecordRepository.sumTotalRevenue()).thenReturn(null);
        when(inventoryDisposalRepository.sumDisposedQuantity()).thenReturn(null);

        ManagerOverviewResponse result = managerDashboardService.getOverview();

        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, result.getTotalDisposedQuantity());
    }

    @Test
    void getKitchenLowStockItems_shouldMapInventoryDetails() {
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").build();
        KitchenInventory item = KitchenInventory.builder()
                .id(11)
                .ingredient(ingredient)
                .quantity(BigDecimal.ONE)
                .minStock(5)
                .unit("kg")
                .build();

        when(kitchenInventoryRepository.findLowStockItems()).thenReturn(List.of(item));

        List<KitchenLowStockResponse> result = managerDashboardService.getKitchenLowStockItems();

        assertEquals(1, result.size());
        assertEquals(11, result.get(0).getInventoryId());
        assertEquals("ING001", result.get(0).getIngredientId());
        assertEquals("Flour", result.get(0).getIngredientName());
    }

    @Test
    void getStoreLowStockItems_shouldMapInventoryDetails() {
        Store store = Store.builder().id("ST001").name("Store A").address("Addr").status("ACTIVE").build();
        Product product = Product.builder().id("PROD001").name("Bread").category(ProductCategory.BAKERY).build();
        StoreInventory item = StoreInventory.builder()
                .id(12)
                .store(store)
                .product(product)
                .quantity(2)
                .minStock(5)
                .unit("piece")
                .build();

        when(storeInventoryRepository.findLowStockItems()).thenReturn(List.of(item));

        List<StoreLowStockResponse> result = managerDashboardService.getStoreLowStockItems();

        assertEquals(1, result.size());
        assertEquals("ST001", result.get(0).getStoreId());
        assertEquals("PROD001", result.get(0).getProductId());
        assertEquals("Bread", result.get(0).getProductName());
    }
}
