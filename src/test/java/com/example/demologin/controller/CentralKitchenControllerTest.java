package com.example.demologin.controller;

import com.example.demologin.dto.request.centralkitchen.CancelPlanRequest;
import com.example.demologin.dto.request.centralkitchen.CompletePlanRequest;
import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateBatchRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.BatchResponse;
import com.example.demologin.dto.response.CentralKitchenOverviewResponse;
import com.example.demologin.dto.response.KitchenInventoryDetailResponse;
import com.example.demologin.dto.response.KitchenProductInventoryResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.ProductionPlanResponse;
import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.dto.response.RecipeCheckResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.service.CentralKitchenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CentralKitchenControllerTest {

    @Mock
    private CentralKitchenService centralKitchenService;

    @InjectMocks
    private CentralKitchenController controller;

    private Principal principal;
    private LocalDate fromDate;
    private LocalDate toDate;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        fromDate = LocalDate.of(2026, 4, 1);
        toDate = LocalDate.of(2026, 4, 30);
    }

    // ==================== ORDER ENDPOINTS ====================

    @Test
    void getOrders_shouldInvokeService() {
        Page<OrderResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getAllOrders("PENDING", "ST001", 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getOrders("PENDING", "ST001", 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getAllOrders("PENDING", "ST001", 0, 20, principal);
    }

    @Test
    void getOrders_withNullFilters_shouldInvokeService() {
        Page<OrderResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getAllOrders(null, null, 5, 10, principal))
                .thenReturn(expectedPage);

        Object result = controller.getOrders(null, null, 5, 10, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getAllOrders(null, null, 5, 10, principal);
    }

    @Test
    void getOrderById_shouldInvokeService() {
        OrderResponse expectedResponse = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.getOrderById("ORD001", principal)).thenReturn(expectedResponse);

        Object result = controller.getOrderById("ORD001", principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getOrderById("ORD001", principal);
    }

    @Test
    void assignOrder_shouldInvokeService() {
        OrderResponse expectedResponse = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.assignOrder("ORD001", principal)).thenReturn(expectedResponse);

        Object result = controller.assignOrder("ORD001", principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).assignOrder("ORD001", principal);
    }

    @Test
    void updateOrderStatus_shouldInvokeService() {
        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        OrderResponse expectedResponse = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.updateOrderStatus("ORD001", request, principal))
                .thenReturn(expectedResponse);

        Object result = controller.updateOrderStatus("ORD001", request, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).updateOrderStatus("ORD001", request, principal);
    }

    @Test
    void getOrderStatuses_shouldInvokeService() {
        List<String> expectedStatuses = List.of("IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING");
        when(centralKitchenService.getOrderStatuses(principal)).thenReturn(expectedStatuses);

        Object result = controller.getOrderStatuses(principal);

        assertSame(expectedStatuses, result);
        verify(centralKitchenService).getOrderStatuses(principal);
    }

    // ==================== PRODUCTION PLAN ENDPOINTS ====================

    @Test
    void getProductionPlans_shouldInvokeService() {
        Page<ProductionPlanResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductionPlans(0, 20, principal)).thenReturn(expectedPage);

        Object result = controller.getProductionPlans(0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductionPlans(0, 20, principal);
    }

    @Test
    void getProductionPlans_withCustomPagination_shouldInvokeService() {
        Page<ProductionPlanResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductionPlans(2, 15, principal)).thenReturn(expectedPage);

        Object result = controller.getProductionPlans(2, 15, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductionPlans(2, 15, principal);
    }

    @Test
    void createProductionPlan_shouldInvokeService() {
        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001")
                .quantity(100)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        ProductionPlanResponse expectedResponse = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.createProductionPlan(request, principal)).thenReturn(expectedResponse);

        Object result = controller.createProductionPlan(request, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).createProductionPlan(request, principal);
    }

    @Test
    void getProductionPlanById_shouldInvokeService() {
        ProductionPlanResponse expectedResponse = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.getProductionPlanById("PLN001", principal)).thenReturn(expectedResponse);

        Object result = controller.getProductionPlanById("PLN001", principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getProductionPlanById("PLN001", principal);
    }

    @Test
    void startProductionPlan_shouldInvokeService() {
        ProductionPlanResponse expectedResponse = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.startProductionPlan("PLN001", principal)).thenReturn(expectedResponse);

        Object result = controller.startProductionPlan("PLN001", principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).startProductionPlan("PLN001", principal);
    }

    @Test
    void completeProductionPlan_shouldInvokeService() {
        CompletePlanRequest request = new CompletePlanRequest();
        ProductionPlanResponse expectedResponse = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.completeProductionPlan(eq("PLN001"), any(), any(), eq(principal)))
                .thenReturn(expectedResponse);

        Object result = controller.completeProductionPlan("PLN001", request, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).completeProductionPlan(eq("PLN001"), any(), any(), eq(principal));
    }

    @Test
    void cancelProductionPlan_shouldInvokeService() {
        CancelPlanRequest request = new CancelPlanRequest();
        ProductionPlanResponse expectedResponse = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.cancelProductionPlan("PLN001", null, principal)).thenReturn(expectedResponse);

        Object result = controller.cancelProductionPlan("PLN001", request, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).cancelProductionPlan("PLN001", null, principal);
    }

    // ==================== INVENTORY ENDPOINTS ====================

    @Test
    void getInventory_shouldInvokeService() {
        Page<KitchenInventoryDetailResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getInventory("ING001", "Bột", false, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getInventory("ING001", "Bột", false, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getInventory("ING001", "Bột", false, 0, 20, principal);
    }

    @Test
    void getInventory_withLowStockFilter_shouldInvokeService() {
        Page<KitchenInventoryDetailResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getInventory(null, null, true, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getInventory(null, null, true, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getInventory(null, null, true, 0, 20, principal);
    }

    // ==================== PRODUCT BATCH ENDPOINTS ====================

    @Test
    void getProductBatches_shouldInvokeService() {
        Page<BatchResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductBatches("PROD001", "AVAILABLE", 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProductBatches("PROD001", "AVAILABLE", 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductBatches("PROD001", "AVAILABLE", 0, 20, principal);
    }

    @Test
    void getProductBatches_withNullFilters_shouldInvokeService() {
        Page<BatchResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductBatches(null, null, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProductBatches(null, null, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductBatches(null, null, 0, 20, principal);
    }

    @Test
    void getProductBatchById_shouldInvokeService() {
        BatchResponse expectedResponse = BatchResponse.builder().id("BATCH001").build();
        when(centralKitchenService.getProductBatchById("BATCH001", principal)).thenReturn(expectedResponse);

        Object result = controller.getProductBatchById("BATCH001", principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getProductBatchById("BATCH001", principal);
    }

    @Test
    void updateBatch_shouldInvokeService() {
        UpdateBatchRequest request = new UpdateBatchRequest();
        BatchResponse expectedResponse = BatchResponse.builder().id("BATCH001").build();
        when(centralKitchenService.updateBatch("BATCH001", request, principal)).thenReturn(expectedResponse);

        Object result = controller.updateBatch("BATCH001", request, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).updateBatch("BATCH001", request, principal);
    }

    // ==================== KITCHEN & OVERVIEW ENDPOINTS ====================

    @Test
    void getMyKitchen_shouldInvokeService() {
        KitchenResponse expectedResponse = KitchenResponse.builder().id("KIT001").build();
        when(centralKitchenService.getMyKitchen(principal)).thenReturn(expectedResponse);

        Object result = controller.getMyKitchen(principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getMyKitchen(principal);
    }

    @Test
    void getOverview_shouldInvokeService() {
        CentralKitchenOverviewResponse expectedResponse = CentralKitchenOverviewResponse.builder()
                .kitchenId("KIT001")
                .build();
        when(centralKitchenService.getOverview(fromDate, toDate, principal)).thenReturn(expectedResponse);

        Object result = controller.getOverview(fromDate, toDate, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getOverview(fromDate, toDate, principal);
    }

    @Test
    void getOverview_withNullDates_shouldInvokeService() {
        CentralKitchenOverviewResponse expectedResponse = CentralKitchenOverviewResponse.builder()
                .kitchenId("KIT001")
                .build();
        when(centralKitchenService.getOverview(null, null, principal)).thenReturn(expectedResponse);

        Object result = controller.getOverview(null, null, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).getOverview(null, null, principal);
    }

    // ==================== STORE & PRODUCT ENDPOINTS ====================

    @Test
    void getStores_shouldInvokeService() {
        Page<StoreResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getStores("District", "ACTIVE", 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getStores("District", "ACTIVE", 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getStores("District", "ACTIVE", 0, 20, principal);
    }

    @Test
    void getStores_withNullFilters_shouldInvokeService() {
        Page<StoreResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getStores(null, null, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getStores(null, null, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getStores(null, null, 0, 20, principal);
    }

    @Test
    void getProducts_shouldInvokeService() {
        Page<ProductResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProducts("Bánh", "BAKERY", 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProducts("Bánh", "BAKERY", 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProducts("Bánh", "BAKERY", 0, 20, principal);
    }

    @Test
    void getProducts_withNullFilters_shouldInvokeService() {
        Page<ProductResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProducts(null, null, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProducts(null, null, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProducts(null, null, 0, 20, principal);
    }

    @Test
    void getProductInventory_shouldInvokeService() {
        Page<KitchenProductInventoryResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductInventory("PROD001", "Bánh", 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProductInventory("PROD001", "Bánh", 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductInventory("PROD001", "Bánh", 0, 20, principal);
    }

    @Test
    void getProductInventory_withNullFilters_shouldInvokeService() {
        Page<KitchenProductInventoryResponse> expectedPage = new PageImpl<>(List.of());
        when(centralKitchenService.getProductInventory(null, null, 0, 20, principal))
                .thenReturn(expectedPage);

        Object result = controller.getProductInventory(null, null, 0, 20, principal);

        assertSame(expectedPage, result);
        verify(centralKitchenService).getProductInventory(null, null, 0, 20, principal);
    }

    @Test
    void checkRecipeAvailability_shouldInvokeService() {
        RecipeCheckResponse expectedResponse = RecipeCheckResponse.builder()
                .productId("PROD001")
                .productName("Bánh Mì")
                .requestedQuantity(100)
                .canProduce(true)
                .build();
        when(centralKitchenService.checkRecipeAvailability("PROD001", 100, principal))
                .thenReturn(expectedResponse);

        Object result = controller.checkRecipeAvailability("PROD001", 100, principal);

        assertSame(expectedResponse, result);
        verify(centralKitchenService).checkRecipeAvailability("PROD001", 100, principal);
    }
}