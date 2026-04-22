package com.example.demologin.controller;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.CentralKitchenOverviewResponse;
import com.example.demologin.dto.response.KitchenInventoryDetailResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.ProductionPlanResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.service.CentralKitchenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CentralKitchenControllerTest {

    @Mock
    private CentralKitchenService centralKitchenService;

    @InjectMocks
    private CentralKitchenController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getOrders_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(centralKitchenService.getAllOrders("PENDING", "ST001", 0, 20, principal)).thenReturn(page);

        Object result = controller.getOrders("PENDING", "ST001", 0, 20, principal);

        assertSame(page, result);
        verify(centralKitchenService).getAllOrders("PENDING", "ST001", 0, 20, principal);
    }

    @Test
    void getOrderById_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.getOrderById("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderById("ORD001", principal);

        assertSame(response, result);
        verify(centralKitchenService).getOrderById("ORD001", principal);
    }

    @Test
    void assignOrder_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.assignOrder("ORD001", principal)).thenReturn(response);

        Object result = controller.assignOrder("ORD001", principal);

        assertSame(response, result);
        verify(centralKitchenService).assignOrder("ORD001", principal);
    }

    @Test
    void updateOrderStatus_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(centralKitchenService.updateOrderStatus("ORD001", request, principal)).thenReturn(response);

        Object result = controller.updateOrderStatus("ORD001", request, principal);

        assertSame(response, result);
        verify(centralKitchenService).updateOrderStatus("ORD001", request, principal);
    }

    @Test
    void getProductionPlans_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        Page<ProductionPlanResponse> page = new PageImpl<>(List.of());
        when(centralKitchenService.getProductionPlans(0, 20, principal)).thenReturn(page);

        Object result = controller.getProductionPlans(0, 20, principal);

        assertSame(page, result);
        verify(centralKitchenService).getProductionPlans(0, 20, principal);
    }

    @Test
    void createProductionPlan_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        CreateProductionPlanRequest request = mock(CreateProductionPlanRequest.class);
        ProductionPlanResponse response = ProductionPlanResponse.builder().id("PLN001").build();
        when(centralKitchenService.createProductionPlan(request, principal)).thenReturn(response);

        Object result = controller.createProductionPlan(request, principal);

        assertSame(response, result);
        verify(centralKitchenService).createProductionPlan(request, principal);
    }

    @Test
    void getInventory_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        Page<KitchenInventoryDetailResponse> page = new PageImpl<>(List.of());
        when(centralKitchenService.getInventory("ING001", "Bột", false, 0, 20, principal)).thenReturn(page);

        Object result = controller.getInventory("ING001", "Bột", false, 0, 20, principal);

        assertSame(page, result);
        verify(centralKitchenService).getInventory("ING001", "Bột", false, 0, 20, principal);
    }

    @Test
    void getMyKitchen_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        KitchenResponse response = KitchenResponse.builder().id("KIT001").build();
        when(centralKitchenService.getMyKitchen(principal)).thenReturn(response);

        Object result = controller.getMyKitchen(principal);

        assertSame(response, result);
        verify(centralKitchenService).getMyKitchen(principal);
    }

    @Test
    void getOrderStatuses_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        List<String> statuses = List.of("IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING");
        when(centralKitchenService.getOrderStatuses(principal)).thenReturn(statuses);

        Object result = controller.getOrderStatuses(principal);

        assertSame(statuses, result);
        verify(centralKitchenService).getOrderStatuses(principal);
    }

    @Test
    void getOverview_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        CentralKitchenOverviewResponse response = CentralKitchenOverviewResponse.builder().kitchenId("KIT001").build();
        when(centralKitchenService.getOverview(fromDate, toDate, principal)).thenReturn(response);

        Object result = controller.getOverview(fromDate, toDate, principal);

        assertSame(response, result);
        verify(centralKitchenService).getOverview(fromDate, toDate, principal);
    }

    @Test
    void getStores_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        Page<StoreResponse> page = new PageImpl<>(List.of());
        when(centralKitchenService.getStores("District", "ACTIVE", 0, 20, principal)).thenReturn(page);

        Object result = controller.getStores("District", "ACTIVE", 0, 20, principal);

        assertSame(page, result);
        verify(centralKitchenService).getStores("District", "ACTIVE", 0, 20, principal);
    }
}
