package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.manager.KitchenInventoryUpsertRequest;
import com.example.demologin.dto.response.KitchenInventoryResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.manager.IngredientFilterOptionResponse;
import com.example.demologin.dto.response.manager.ManagerKitchenInventoryGroupResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.KitchenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerInventoryServiceImplTest {

    @Mock
    private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private KitchenRepository kitchenRepository;

    @InjectMocks
    private ManagerInventoryServiceImpl service;

    private Kitchen kitchen;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();
        ingredient = Ingredient.builder().id("ING001").name("Ingredient 1").unit("kg").build();
    }

    @Test
    void getKitchenInventory_FilterByKitchenId() {
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        KitchenInventory inventory = KitchenInventory.builder()
                .id(1)
                .kitchen(kitchen)
                .ingredient(ingredient)
                .quantity(BigDecimal.TEN)
                .minStock(5)
                .build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(inventory));

        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory("KIT001", null, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("KIT001", result.getContent().get(0).getKitchenId());
        verify(kitchenRepository).findById("KIT001");
    }

    @Test
    void getKitchenInventory_PaginationLogic() {
        Kitchen k2 = Kitchen.builder().id("KIT002").name("Kitchen 2").build();
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen, k2));
        
        KitchenInventory item = KitchenInventory.builder().id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.TEN).minStock(5).build();
        KitchenInventory item2 = KitchenInventory.builder().id(2).kitchen(k2).ingredient(ingredient).quantity(BigDecimal.TEN).minStock(5).build();
        
        // Mock for each group mapping
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(item))   // For KIT001
                .thenReturn(List.of(item2));  // For KIT002

        // Test page size 1
        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory(null, null, 0, 1);
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals("KIT001", result.getContent().get(0).getKitchenId());

        // Test page 1
        Page<ManagerKitchenInventoryGroupResponse> resultPage1 = service.getKitchenInventory(null, null, 1, 1);
        assertEquals(1, resultPage1.getContent().size());
        assertEquals("KIT002", resultPage1.getContent().get(0).getKitchenId());
    }

    @Test
    void createKitchenInventory_Success() {
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING001");
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMinStock(10);
        request.setBatchNo("B1");
        request.setSupplier("S1");

        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING001")).thenReturn(Optional.of(ingredient));
        when(kitchenInventoryRepository.save(any(KitchenInventory.class))).thenAnswer(i -> {
            KitchenInventory saved = i.getArgument(0);
            saved.setId(123);
            return saved;
        });

        KitchenInventoryResponse response = service.createKitchenInventory(request);

        assertNotNull(response);
        assertEquals(123, response.getId());
        assertEquals("kg", response.getUnit()); // Derived from ingredient
        verify(kitchenInventoryRepository).save(any(KitchenInventory.class));
    }

    @Test
    void createKitchenInventory_KitchenNotFound() {
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT404");
        when(kitchenRepository.findById("KIT404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createKitchenInventory(request));
    }

    @Test
    void updateKitchenInventory_Success() {
        KitchenInventory existing = KitchenInventory.builder()
                .id(1)
                .kitchen(kitchen)
                .ingredient(ingredient)
                .quantity(BigDecimal.TEN)
                .build();
        
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING001");
        request.setQuantity(BigDecimal.valueOf(50));
        request.setMinStock(5);

        when(kitchenInventoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING001")).thenReturn(Optional.of(ingredient));
        when(kitchenInventoryRepository.save(any(KitchenInventory.class))).thenAnswer(i -> i.getArgument(0));

        KitchenInventoryResponse response = service.updateKitchenInventory(1, request);

        assertEquals(BigDecimal.valueOf(50), response.getQuantity());
        verify(kitchenInventoryRepository).save(existing);
    }

    @Test
    void deleteKitchenInventory_Success() {
        KitchenInventory existing = KitchenInventory.builder().id(1).build();
        when(kitchenInventoryRepository.findById(1)).thenReturn(Optional.of(existing));

        service.deleteKitchenInventory(1);

        verify(kitchenInventoryRepository).delete(existing);
    }

    @Test
    void deleteKitchenInventory_NotFound() {
        when(kitchenInventoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteKitchenInventory(1));
    }

    @Test
    void getAllKitchens() {
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen));
        
        List<KitchenResponse> result = service.getAllKitchens();
        
        assertEquals(1, result.size());
        assertEquals("KIT001", result.get(0).getId());
    }

    @Test
    void getAllIngredientsForFilter() {
        when(ingredientRepository.findAll(any(Sort.class))).thenReturn(List.of(ingredient));
        
        List<IngredientFilterOptionResponse> result = service.getAllIngredientsForFilter();
        
        assertEquals(1, result.size());
        assertEquals("ING001", result.get(0).getId());
        assertEquals("kg", result.get(0).getUnit());
    }

    @Test
    void getKitchenInventory_WithLowStockFilter_True() {
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen));
        KitchenInventory lowStockItem = KitchenInventory.builder()
                .id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.ONE).minStock(5).build();
        
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(lowStockItem));

        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory(null, true, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(true, result.getContent().get(0).getItems().get(0).isLowStock());
    }

    @Test
    void getKitchenInventory_WithLowStockFilter_False() {
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen));
        KitchenInventory normalStockItem = KitchenInventory.builder()
                .id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.TEN).minStock(5).build();
        
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(normalStockItem));

        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory(null, false, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(false, result.getContent().get(0).getItems().get(0).isLowStock());
    }

    @Test
    void getKitchenInventory_ExcludeKitchensWithNoItems() {
        Kitchen k2 = Kitchen.builder().id("KIT002").name("Kitchen 2").build();
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen, k2));
        
        // Mock: KIT001 has items, KIT002 has none
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(KitchenInventory.builder().id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.TEN).minStock(5).build()))
                .thenReturn(List.of()); // KIT002

        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory(null, null, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("KIT001", result.getContent().get(0).getKitchenId());
    }

    @Test
    void createKitchenInventory_IngredientNotFound() {
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING404");
        
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createKitchenInventory(request));
    }

    @Test
    void updateKitchenInventory_IngredientNotFound() {
        KitchenInventory existing = KitchenInventory.builder().id(1).build();
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING404");

        when(kitchenInventoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateKitchenInventory(1, request));
    }

    @Test
    void toKitchenInventoryResponse_HandleNullKitchen() {
        // Test case where kitchen property is null
        KitchenInventory inventory = KitchenInventory.builder()
                .id(1)
                .kitchen(null)
                .ingredient(ingredient)
                .quantity(BigDecimal.TEN)
                .minStock(5)
                .build();
        
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING001");
        request.setQuantity(BigDecimal.TEN);
        request.setMinStock(5);

        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING001")).thenReturn(Optional.of(ingredient));
        when(kitchenInventoryRepository.save(any(KitchenInventory.class))).thenReturn(inventory);

        KitchenInventoryResponse response = service.createKitchenInventory(request);

        assertNull(response.getKitchenId());
        assertNull(response.getKitchenName());
    }

    @Test
    void lowStockCalculation_BoundaryCases() {
        // Equal to minStock -> lowStock = true
        KitchenInventory item1 = KitchenInventory.builder().id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.valueOf(5)).minStock(5).build();
        when(kitchenInventoryRepository.findById(1)).thenReturn(Optional.of(item1));
        
        // Greater than minStock -> lowStock = false
        KitchenInventory item2 = KitchenInventory.builder().id(2).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.valueOf(6)).minStock(5).build();
        when(kitchenInventoryRepository.findById(2)).thenReturn(Optional.of(item2));

        when(kitchenRepository.findById(anyString())).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById(anyString())).thenReturn(Optional.of(ingredient));
        
        // Case 1: Quantity 5, MinStock 5 -> lowStock = true
        KitchenInventoryUpsertRequest req1 = new KitchenInventoryUpsertRequest();
        req1.setKitchenId("K"); req1.setIngredientId("I"); req1.setQuantity(BigDecimal.valueOf(5)); req1.setMinStock(5);
        when(kitchenInventoryRepository.save(item1)).thenReturn(item1);
        KitchenInventoryResponse res1 = service.updateKitchenInventory(1, req1);
        assertTrue(res1.isLowStock());

        // Case 2: Quantity 10, MinStock 5 -> lowStock = false
        KitchenInventoryUpsertRequest req2 = new KitchenInventoryUpsertRequest();
        req2.setKitchenId("K"); req2.setIngredientId("I"); req2.setQuantity(BigDecimal.valueOf(10)); req2.setMinStock(5);
        when(kitchenInventoryRepository.save(item2)).thenReturn(item2);
        KitchenInventoryResponse res2 = service.updateKitchenInventory(2, req2);
        assertFalse(res2.isLowStock());
    }

    @Test
    void getKitchenInventory_DefaultSize() {
        // Mock 25 kitchens to test default size 20 logic
        java.util.ArrayList<Kitchen> manyKitchens = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            manyKitchens.add(Kitchen.builder().id("K" + i).name("Kitchen " + i).build());
        }
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(manyKitchens);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(KitchenInventory.builder().id(1).ingredient(ingredient).quantity(BigDecimal.TEN).build()));

        // Test with size = 0, should default to 20
        Page<ManagerKitchenInventoryGroupResponse> result = service.getKitchenInventory(null, null, 0, 0);

        assertEquals(20, result.getContent().size());
        assertEquals(25, result.getTotalElements());
    }

    @Test
    void updateKitchenInventory_NotFound() {
        when(kitchenInventoryRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateKitchenInventory(999, new KitchenInventoryUpsertRequest()));
    }

    @Test
    void normalizeText_EmptyString_To_Null() {
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING001");
        request.setQuantity(BigDecimal.TEN);
        request.setMinStock(5);
        request.setBatchNo("   "); // Should become null
        request.setSupplier("");    // Should become null

        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING001")).thenReturn(Optional.of(ingredient));
        when(kitchenInventoryRepository.save(any(KitchenInventory.class))).thenAnswer(i -> i.getArgument(0));

        KitchenInventoryResponse response = service.createKitchenInventory(request);

        assertNull(response.getBatchNo());
        assertNull(response.getSupplier());
    }

    @Test
    void toKitchenInventoryResponse_NullQuantityOrMinStock() {
        KitchenInventory inventory = KitchenInventory.builder()
                .id(1)
                .kitchen(kitchen)
                .ingredient(ingredient)
                .quantity(null)   // Null quantity
                .minStock(null)   // Null minStock
                .build();
        
        KitchenInventoryUpsertRequest request = new KitchenInventoryUpsertRequest();
        request.setKitchenId("KIT001");
        request.setIngredientId("ING001");
        request.setQuantity(BigDecimal.TEN);
        request.setMinStock(5);

        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(ingredientRepository.findById("ING001")).thenReturn(Optional.of(ingredient));
        when(kitchenInventoryRepository.save(any(KitchenInventory.class))).thenReturn(inventory);

        KitchenInventoryResponse response = service.createKitchenInventory(request);

        assertFalse(response.isLowStock()); // Should be false if either is null
    }

    @Test
    void toKitchenGroupResponse_lowStockSpec_Branches() {
        // Already covered mostly, but let's ensure we hit the TRUE/FALSE branches in toKitchenGroupResponse Specification logic
        when(kitchenRepository.findAll(any(Sort.class))).thenReturn(List.of(kitchen));
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(KitchenInventory.builder().id(1).kitchen(kitchen).ingredient(ingredient).quantity(BigDecimal.TEN).minStock(5).build()));

        // Call once with Boolean.TRUE and once with Boolean.FALSE
        service.getKitchenInventory(null, true, 0, 10);
        service.getKitchenInventory(null, false, 0, 10);
        
        verify(kitchenInventoryRepository, atLeast(2)).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getAllSuppliersForFilter() {
        when(kitchenInventoryRepository.findDistinctSuppliers()).thenReturn(List.of("S1", "S2"));
        
        List<String> result = service.getAllSuppliersForFilter();
        
        assertEquals(2, result.size());
        assertTrue(result.contains("S1"));
    }
}


