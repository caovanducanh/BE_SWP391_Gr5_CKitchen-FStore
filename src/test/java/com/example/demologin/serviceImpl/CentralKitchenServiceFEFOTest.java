package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.ProductionPlanResponse;
import com.example.demologin.entity.*;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralKitchenServiceFEFOTest {

    @Mock private ProductionPlanRepository productionPlanRepository;
    @Mock private PlanIngredientRepository planIngredientRepository;
    @Mock private IngredientBatchRepository ingredientBatchRepository;
    @Mock private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock private PlanIngredientBatchUsageRepository planIngredientBatchUsageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private BatchRepository batchRepository;

    @InjectMocks
    private CentralKitchenServiceImpl centralKitchenService;

    private Principal mockPrincipal;
    private User mockStaff;
    private Kitchen mockKitchen;
    private ProductionPlan mockPlan;
    private PlanIngredient mockPlanIngredient;
    private Ingredient mockIngredient;

    @BeforeEach
    void setUp() {
        mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("staff");

        mockKitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();

        Role role = Role.builder().name("CENTRAL_KITCHEN_STAFF").build();
        mockStaff = new User();
        mockStaff.setUsername("staff");
        mockStaff.setKitchen(mockKitchen);
        mockStaff.setRole(role);

        mockIngredient = Ingredient.builder().id("ING001").name("Sugar").unit("kg").build();

        Product mockProduct = Product.builder().id("PROD001").name("Product 1").build();
        mockPlan = ProductionPlan.builder()
                .id("PLAN001")
                .kitchen(mockKitchen)
                .product(mockProduct)
                .status("DRAFT")
                .build();

        mockPlanIngredient = PlanIngredient.builder()
                .id(1)
                .plan(mockPlan)
                .ingredient(mockIngredient)
                .quantity(new BigDecimal("15.0"))
                .unit("kg")
                .build();
    }

    @Test
    void startProductionPlan_Success_FEFO_Consumption() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(mockPlan));

        IngredientBatch batch1 = IngredientBatch.builder()
                .id("1").ingredient(mockIngredient).kitchen(mockKitchen).batchNo("B1")
                .remainingQuantity(new BigDecimal("10.0"))
                .build();
        IngredientBatch batch2 = IngredientBatch.builder()
                .id("2").ingredient(mockIngredient).kitchen(mockKitchen).batchNo("B2")
                .remainingQuantity(new BigDecimal("20.0"))
                .build();

        PlanIngredientBatchUsage usage1 = PlanIngredientBatchUsage.builder()
                .id(1).ingredientBatch(batch1).quantityUsed(new BigDecimal("10.0")).build();
        PlanIngredientBatchUsage usage2 = PlanIngredientBatchUsage.builder()
                .id(2).ingredientBatch(batch2).quantityUsed(new BigDecimal("5.0")).build();

        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001"))
                .thenReturn(List.of(usage1, usage2));

        KitchenInventory kitchenInventory = KitchenInventory.builder()
                .id(1).kitchen(mockKitchen).ingredient(mockIngredient).totalQuantity(new BigDecimal("30.0"))
                .build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(kitchenInventory));

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductionPlanResponse response = centralKitchenService.startProductionPlan("PLAN001", mockPrincipal);

        assertEquals("IN_PRODUCTION", mockPlan.getStatus());
        assertEquals("IN_PRODUCTION", response.getStatus());

        assertEquals(new BigDecimal("0.0"), batch1.getRemainingQuantity());
        assertEquals(new BigDecimal("15.0"), batch2.getRemainingQuantity());
        assertEquals("DEPLETED", batch1.getStatus());
        verify(ingredientBatchRepository, times(2)).save(any(IngredientBatch.class));

        assertEquals(new BigDecimal("15.0"), kitchenInventory.getTotalQuantity());
        verify(kitchenInventoryRepository, times(2)).save(kitchenInventory); // called per usage
    }

    @Test
    void startProductionPlan_InsufficientStock_ThrowsException() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(mockPlan));

        IngredientBatch batch1 = IngredientBatch.builder()
                .id("1").ingredient(mockIngredient).kitchen(mockKitchen).batchNo("B1")
                .remainingQuantity(new BigDecimal("5.0")) // Only 5 left!
                .build();

        PlanIngredientBatchUsage usage1 = PlanIngredientBatchUsage.builder()
                .id(1).ingredientBatch(batch1).quantityUsed(new BigDecimal("10.0")).build(); // Try to use 10

        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001"))
                .thenReturn(List.of(usage1));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            centralKitchenService.startProductionPlan("PLAN001", mockPrincipal);
        });

        assertTrue(exception.getMessage().contains("does not have enough qty now"));
        verify(ingredientBatchRepository, never()).saveAll(any());
    }

    @Test
    void cancelProductionPlan_Success_RestoresStock() {
        // Plan is currently IN_PRODUCTION
        mockPlan.setStatus("IN_PRODUCTION");
        
        // Setup user
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(mockPlan));

        // Setup existing usage to restore (Batch 1: 10 used, Batch 2: 5 used)
        IngredientBatch batch1 = IngredientBatch.builder().id("1").ingredient(mockIngredient).kitchen(mockKitchen).status("DEPLETED").remainingQuantity(new BigDecimal("0.0")).build();
        IngredientBatch batch2 = IngredientBatch.builder().id("2").ingredient(mockIngredient).kitchen(mockKitchen).status("ACTIVE").remainingQuantity(new BigDecimal("15.0")).build();
        
        PlanIngredientBatchUsage usage1 = PlanIngredientBatchUsage.builder().id(1).plan(mockPlan).ingredientBatch(batch1).quantityUsed(new BigDecimal("10.0")).build();
        PlanIngredientBatchUsage usage2 = PlanIngredientBatchUsage.builder().id(2).plan(mockPlan).ingredientBatch(batch2).quantityUsed(new BigDecimal("5.0")).build();
        
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage1, usage2));

        // Setup KitchenInventory
        KitchenInventory kitchenInventory = KitchenInventory.builder()
                .id(1).kitchen(mockKitchen).ingredient(mockIngredient).totalQuantity(new BigDecimal("15.0"))
                .build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(kitchenInventory));

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Execute
        ProductionPlanResponse response = centralKitchenService.cancelProductionPlan("PLAN001", "Cancel testing", mockPrincipal);

        // Verify Status
        assertEquals("CANCELLED", mockPlan.getStatus());
        assertEquals("CANCELLED", response.getStatus());

        assertEquals(new BigDecimal("10.0"), batch1.getRemainingQuantity());
        assertEquals("ACTIVE", batch1.getStatus());
        assertEquals(new BigDecimal("20.0"), batch2.getRemainingQuantity());
        verify(ingredientBatchRepository, times(2)).save(any(IngredientBatch.class));

        assertEquals(new BigDecimal("30.0"), kitchenInventory.getTotalQuantity());
        verify(kitchenInventoryRepository, times(2)).save(kitchenInventory);

        // Verify Usage deleted
        verify(planIngredientBatchUsageRepository, times(1)).deleteAll(any());
    }

    @Test
    void createProductionPlan_FEFO_Logic_Success() {
        // Setup input
        com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest request = com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest.builder()
                .productId("PROD001")
                .quantity(10) // Need 10 products
                .startDate(java.time.LocalDateTime.now())
                .endDate(java.time.LocalDateTime.now().plusDays(1))
                .build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        
        Product product = Product.builder().id("PROD001").name("Bánh Mì").unit("ổ").build();
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));

        // Recipe: 1 product needs 2kg of ingredient
        Recipe recipe = Recipe.builder()
                .product(product)
                .ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")) // 2kg per ổ
                .unit("kg")
                .build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        // Need total 10 * 2 = 20kg
        
        // Mock batches:
        // Batch 1: 15kg (Expires soon)
        // Batch 2: 15kg (Expires later)
        IngredientBatch b1 = IngredientBatch.builder().id("B1").ingredient(mockIngredient).remainingQuantity(new BigDecimal("15.0")).expiryDate(LocalDate.now().plusDays(5)).build();
        IngredientBatch b2 = IngredientBatch.builder().id("B2").ingredient(mockIngredient).remainingQuantity(new BigDecimal("15.0")).expiryDate(LocalDate.now().plusDays(10)).build();
        
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc("KIT001", "ING001"))
                .thenReturn(List.of(b1, b2));

        when(productionPlanRepository.save(any())).thenAnswer(i -> {
            ProductionPlan p = i.getArgument(0);
            p.setId("NEW_PLAN");
            return p;
        });

        // Execute
        ProductionPlanResponse response = centralKitchenService.createProductionPlan(request, mockPrincipal);

        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus());
        
        // Verify FEFO calculation
        // Total needed 20kg. B1 has 15kg, B2 has 15kg.
        // Should take 15kg from B1 and 5kg from B2.
        
        verify(planIngredientBatchUsageRepository).saveAll(argThat(usages -> {
            List<PlanIngredientBatchUsage> usageList = (List<PlanIngredientBatchUsage>) usages;
            if (usageList.size() != 2) return false;
            
            PlanIngredientBatchUsage u1 = usageList.stream().filter(u -> u.getIngredientBatch().getId().equals("B1")).findFirst().orElse(null);
            PlanIngredientBatchUsage u2 = usageList.stream().filter(u -> u.getIngredientBatch().getId().equals("B2")).findFirst().orElse(null);
            
            return u1 != null && u1.getQuantityUsed().compareTo(new BigDecimal("15.0")) == 0
                && u2 != null && u2.getQuantityUsed().compareTo(new BigDecimal("5.0")) == 0;
        }));
        
        verify(planIngredientRepository).saveAll(argThat(ingredients -> {
            List<PlanIngredient> piList = (List<PlanIngredient>) ingredients;
            return piList.size() == 1 && piList.get(0).getQuantity().compareTo(new BigDecimal("20.0")) == 0;
        }));
    }

    @Test
    void completeProductionPlan_Success_CreatesProductBatch() {
        // Plan must be IN_PRODUCTION
        mockPlan.setStatus("IN_PRODUCTION");
        mockPlan.setQuantity(50);
        mockPlan.setUnit("ổ");

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(mockPlan));
        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDate expiry = LocalDate.now().plusMonths(1);

        // Execute
        ProductionPlanResponse response = centralKitchenService.completeProductionPlan("PLAN001", "Finished well", expiry, mockPrincipal);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("COMPLETED", mockPlan.getStatus());
        assertTrue(mockPlan.getNotes().contains("Finished well"));

        // Verify product batch (Batch) creation
        verify(batchRepository).save(argThat(batch -> {
            return batch.getProduct().getId().equals("PROD001")
                && batch.getKitchen().getId().equals("KIT001")
                && batch.getQuantity().equals(50)
                && batch.getExpiryDate().equals(expiry)
                && batch.getStatus().equals("AVAILABLE")
                && batch.getId().startsWith("PB");
        }));
    }
}
