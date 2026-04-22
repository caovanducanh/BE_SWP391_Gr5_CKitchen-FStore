package com.example.demologin.controller;


import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.manager.IngredientFilterOptionResponse;
import com.example.demologin.dto.response.manager.ManagerKitchenInventoryGroupResponse;
import com.example.demologin.service.ManagerInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ManagerInventoryControllerTest {

    @Mock
    private ManagerInventoryService managerInventoryService;

    @InjectMocks
    private ManagerInventoryController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getKitchenInventory() {
        Page<ManagerKitchenInventoryGroupResponse> expectedPage = new PageImpl<>(List.of());
        when(managerInventoryService.getKitchenInventory(anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(expectedPage);

        Object result = controller.getKitchenInventory("KIT001", true, 0, 20);

        assertSame(expectedPage, result);
        verify(managerInventoryService).getKitchenInventory("KIT001", true, 0, 20);
    }



    @Test
    void getAllKitchens() {
        List<KitchenResponse> expectedList = List.of();
        when(managerInventoryService.getAllKitchens()).thenReturn(expectedList);

        Object result = controller.getAllKitchens();

        assertSame(expectedList, result);
        verify(managerInventoryService).getAllKitchens();
    }

    @Test
    void getAllIngredientsForFilter() {
        List<IngredientFilterOptionResponse> expectedList = List.of();
        when(managerInventoryService.getAllIngredientsForFilter()).thenReturn(expectedList);

        Object result = controller.getAllIngredientsForFilter();

        assertSame(expectedList, result);
        verify(managerInventoryService).getAllIngredientsForFilter();
    }

    @Test
    void getAllSuppliersForFilter() {
        List<String> expectedList = List.of("Supplier A");
        when(managerInventoryService.getAllSuppliersForFilter()).thenReturn(expectedList);

        Object result = controller.getAllSuppliersForFilter();

        assertSame(expectedList, result);
        verify(managerInventoryService).getAllSuppliersForFilter();
    }
}
