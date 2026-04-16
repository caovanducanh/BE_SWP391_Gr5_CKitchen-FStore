package com.example.demologin.controller;

import com.example.demologin.dto.response.KitchenLowStockResponse;
import com.example.demologin.dto.response.ManagerOverviewResponse;
import com.example.demologin.dto.response.StoreLowStockResponse;
import com.example.demologin.service.ManagerDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerDashboardControllerTest {

    @Mock
    private ManagerDashboardService managerDashboardService;

    @InjectMocks
    private ManagerDashboardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getOverview() {
        ManagerOverviewResponse response = ManagerOverviewResponse.builder().totalProducts(10).build();
        when(managerDashboardService.getOverview()).thenReturn(response);

        Object result = controller.getOverview();

        assertSame(response, result);
        verify(managerDashboardService).getOverview();
    }

    @Test
    void getKitchenLowStock() {
        List<KitchenLowStockResponse> response = List.of(
                KitchenLowStockResponse.builder().inventoryId(1).ingredientName("Flour").build()
        );
        when(managerDashboardService.getKitchenLowStockItems()).thenReturn(response);

        Object result = controller.getKitchenLowStock();

        assertSame(response, result);
        verify(managerDashboardService).getKitchenLowStockItems();
    }

    @Test
    void getStoreLowStock() {
        List<StoreLowStockResponse> response = List.of(
                StoreLowStockResponse.builder().inventoryId(1).productName("Bread").build()
        );
        when(managerDashboardService.getStoreLowStockItems()).thenReturn(response);

        Object result = controller.getStoreLowStock();

        assertSame(response, result);
        verify(managerDashboardService).getStoreLowStockItems();
    }
}
