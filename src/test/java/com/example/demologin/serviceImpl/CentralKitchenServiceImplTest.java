package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateBatchRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.*;
import com.example.demologin.entity.*;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.ProductMapper;
import com.example.demologin.repository.*;
import com.example.demologin.service.IngredientBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CentralKitchenServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ProductionPlanRepository productionPlanRepository;
    @Mock private ProductRepository productRepository;
    @Mock private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private IngredientBatchRepository ingredientBatchRepository;
    @Mock private PlanIngredientRepository planIngredientRepository;
    @Mock private PlanIngredientBatchUsageRepository planIngredientBatchUsageRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private IngredientBatchService ingredientBatchService;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private CentralKitchenServiceImpl centralKitchenService;

    private Principal mockPrincipal;
    private User mockStaff;
    private Kitchen mockKitchen;
    private Store mockStore;
    private Order mockOrder;
    private Order mockPendingOrder;
    private Product mockProduct;
    private Ingredient mockIngredient;

    @BeforeEach
    void setUp() {
        mockPrincipal = mock(Principal.class);

        mockKitchen = Kitchen.builder()
                .id("KIT001").name("Central Kitchen 1")
                .address("123 Main St").phone("0123456789")
                .capacity(1000).status("ACTIVE").build();

        Role role = Role.builder().name("CENTRAL_KITCHEN_STAFF").build();
        mockStaff = new User();
        mockStaff.setUsername("staff");
        mockStaff.setKitchen(mockKitchen);
        mockStaff.setRole(role);

        mockStore = Store.builder()
                .id("ST001").name("Franchise Store 1")
                .address("456 Store St").phone("0987654321")
                .status("ACTIVE").build();

        mockProduct = Product.builder()
                .id("PROD001").name("Bánh Mì").unit("ổ").build();

        mockIngredient = Ingredient.builder()
                .id("ING001").name("Bột mì").unit("kg").build();

        mockOrder = Order.builder()
                .id("ORD001").store(mockStore)
                .status(OrderStatus.IN_PROGRESS).priority("NORMAL")
                .total(new BigDecimal("100000")).createdBy("storeStaff").build();

        mockPendingOrder = Order.builder()
                .id("ORD002").store(mockStore)
                .status(OrderStatus.PENDING).priority("NORMAL")
                .total(new BigDecimal("100000")).createdBy("storeStaff").build();
    }

    // ==================== ORDER TESTS ====================

    @Test
    void getOrderById_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        OrderResponse result = centralKitchenService.getOrderById("ORD001", mockPrincipal);
        assertNotNull(result);
        assertEquals("ORD001", result.getId());
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.getOrderById("ORD999", mockPrincipal));
    }

    @Test
    void assignOrder_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD002")).thenReturn(Optional.of(mockPendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockPendingOrder);
        when(orderItemRepository.findByOrder_Id("ORD002")).thenReturn(new ArrayList<>());

        OrderResponse result = centralKitchenService.assignOrder("ORD002", mockPrincipal);
        assertNotNull(result);
        assertEquals(OrderStatus.ASSIGNED, mockPendingOrder.getStatus());
    }

    @Test
    void assignOrder_KitchenNull_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.assignOrder("ORD001", mockPrincipal));
    }

    @Test
    void assignOrder_InvalidStatus_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.assignOrder("ORD001", mockPrincipal));
    }

    @Test
    void assignOrder_OrderNotFound_ThrowsNotFoundException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_NOTFOUND")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.assignOrder("ORD_NOTFOUND", mockPrincipal));
    }

    // ==================== UPDATE ORDER STATUS TESTS ====================

    @Test
    void updateOrderStatus_ValidTransition_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        // Setup batch for deduction when transitioning to PACKED_WAITING_SHIPPER
        OrderItem orderItem = OrderItem.builder()
                .id(1).product(mockProduct).quantity(5).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        Batch batch = Batch.builder()
                .id("B001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);
        assertNotNull(result);
        verify(orderRepository).save(mockOrder);
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order pendingOrder = Order.builder()
                .id("ORD003").store(mockStore).status(OrderStatus.PENDING).build();
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD003")).thenReturn(Optional.of(pendingOrder));

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.DELIVERED);

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.updateOrderStatus("ORD003", request, mockPrincipal));
    }

    @Test
    void updateOrderStatus_OrderNotFound_ThrowsNotFoundException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_MISSING")).thenReturn(Optional.empty());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.updateOrderStatus("ORD_MISSING", request, mockPrincipal));
    }

    /**
     * FIX 1.1: Nhánh else của if(!currentStatus.equals(newStatus)) — status không thay đổi.
     * Không gọi handleProductDeductionOnPacking, không gọi markOrderStatusTimestamp.
     */
    @Test
    void updateOrderStatus_SameStatus_NoTransitionLogic_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS); // Same as current
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PROGRESS, mockOrder.getStatus());
        // Batch deduction should NOT be triggered
        verify(batchRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    /**
     * FIX 1.2 + FIX 14.1: notes không null, oldNotes == null → appendInternalNote tạo note mới.
     */
    @Test
    void updateOrderStatus_WithNotes_OldNotesNull_CreatesNewNote() {
        Order order = Order.builder()
                .id("ORD_NONOTES").store(mockStore)
                .status(OrderStatus.IN_PROGRESS).notes(null).build();

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_NONOTES")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrder_Id("ORD_NONOTES")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS); // Same status, tránh deduction
        when(request.getNotes()).thenReturn("Ghi chú nội bộ");

        centralKitchenService.updateOrderStatus("ORD_NONOTES", request, mockPrincipal);

        assertNotNull(order.getNotes());
        assertTrue(order.getNotes().contains("Ghi chú nội bộ"));
    }

    /**
     * FIX 14.2: notes không null, oldNotes đã có giá trị → appendInternalNote nối thêm.
     */
    @Test
    void updateOrderStatus_WithNotes_OldNotesHasValue_AppendsNote() {
        Order order = Order.builder()
                .id("ORD_HASNOTES").store(mockStore)
                .status(OrderStatus.IN_PROGRESS).notes("Note cũ").build();

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_HASNOTES")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrder_Id("ORD_HASNOTES")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn("Note mới");

        centralKitchenService.updateOrderStatus("ORD_HASNOTES", request, mockPrincipal);

        assertTrue(order.getNotes().contains("Note cũ"));
        assertTrue(order.getNotes().contains("Note mới"));
    }

    @Test
    void getOrderStatuses_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        List<String> result = centralKitchenService.getOrderStatuses(mockPrincipal);
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    void getOrderStatuses_ContainsExpectedValues() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        List<String> result = centralKitchenService.getOrderStatuses(mockPrincipal);
        assertTrue(result.contains("IN_PROGRESS"));
    }

    // ==================== GET ALL ORDERS TESTS ====================

    @Test
    void getAllOrders_WithStatusAndStoreId_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(storeRepository.existsById("ST001")).thenReturn(true);

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByStore_IdAndStatus(eq("ST001"), eq(OrderStatus.IN_PROGRESS), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("IN_PROGRESS", "ST001", 0, 20, mockPrincipal);
        assertNotNull(result);
        verify(orderRepository).findByStore_IdAndStatus(eq("ST001"), eq(OrderStatus.IN_PROGRESS), any(PageRequest.class));
    }

    @Test
    void getAllOrders_WithStatusOnly_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByStatus(eq(OrderStatus.IN_PROGRESS), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("IN_PROGRESS", null, 0, 20, mockPrincipal);
        assertNotNull(result);
        verify(orderRepository).findByStatus(eq(OrderStatus.IN_PROGRESS), any(PageRequest.class));
    }

    @Test
    void getAllOrders_WithStoreIdOnly_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(storeRepository.existsById("ST001")).thenReturn(true);

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        Page<OrderResponse> result = centralKitchenService.getAllOrders(null, "ST001", 0, 20, mockPrincipal);
        assertNotNull(result);
        verify(orderRepository).findByStore_Id(eq("ST001"), any(PageRequest.class));
    }

    @Test
    void getAllOrders_NoFilters_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        Page<OrderResponse> result = centralKitchenService.getAllOrders(null, null, 0, 20, mockPrincipal);
        assertNotNull(result);
        verify(orderRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getAllOrders_StoreNotFound_ThrowsNotFound() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(storeRepository.existsById("ST999")).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.getAllOrders(null, "ST999", 0, 20, mockPrincipal));
    }

    @Test
    void getAllOrders_InvalidStatusString_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getAllOrders("INVALID_STATUS", null, 0, 20, mockPrincipal));
    }

    @Test
    void getAllOrders_EmptyResult_ReturnsEmptyPage() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Order> emptyPage = new PageImpl<>(new ArrayList<>());
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        Page<OrderResponse> result = centralKitchenService.getAllOrders(null, null, 0, 20, mockPrincipal);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== PRODUCTION PLAN TESTS ====================

    @Test
    void getProductionPlans_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").product(mockProduct).kitchen(mockKitchen).build();
        Page<ProductionPlan> planPage = new PageImpl<>(List.of(plan));
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(planPage);
        when(planIngredientRepository.findByPlan_Id(anyString())).thenReturn(new ArrayList<>());

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void getProductionPlans_EmptyResult_ReturnsEmptyPage() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<ProductionPlan> emptyPage = new PageImpl<>(new ArrayList<>());
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, mockPrincipal);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProductionPlanById_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN001")).thenReturn(Optional.of(plan));
        when(planIngredientRepository.findByPlan_Id("PLN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.getProductionPlanById("PLN001", mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void getProductionPlanById_NotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLN999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.getProductionPlanById("PLN999", mockPrincipal));
    }

    @Test
    void getProductionPlanById_WrongKitchen_ThrowsException() {
        Kitchen otherKitchen = Kitchen.builder().id("KIT002").build();
        mockStaff.setKitchen(otherKitchen);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN001")).thenReturn(Optional.of(plan));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProductionPlanById("PLN001", mockPrincipal));
    }

    /**
     * FIX 10.2 + 10.3: toProductionPlanResponse — pis không rỗng, available == null từ DB.
     */
    @Test
    void getProductionPlanById_WithIngredients_AvailableNull_TreatedAsZero() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN001")).thenReturn(Optional.of(plan));

        PlanIngredient pi = PlanIngredient.builder()
                .id(1).plan(plan).ingredient(mockIngredient)
                .quantity(new BigDecimal("10.0")).unit("kg").build();
        when(planIngredientRepository.findByPlan_Id("PLN001")).thenReturn(List.of(pi));

        // sumRemainingByKitchenAndIngredient trả về null → phải fallback về BigDecimal.ZERO
        when(ingredientBatchRepository.sumRemainingByKitchenAndIngredient(anyString(), anyString()))
                .thenReturn(null);

        ProductionPlanResponse result = centralKitchenService.getProductionPlanById("PLN001", mockPrincipal);

        assertNotNull(result);
        assertNotNull(result.getIngredients());
        assertEquals(1, result.getIngredients().size());
        assertEquals(BigDecimal.ZERO, result.getIngredients().get(0).getAvailableQuantity());
        assertFalse(result.getIngredients().get(0).isSufficient());
    }

    /**
     * FIX 10.1: toProductionPlanResponse — plan.getId() == null → ingredientResponses = null.
     */
    @Test
    void getProductionPlans_PlanWithNullId_ReturnsNullIngredients() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        // Plan không có ID (edge case)
        ProductionPlan plan = ProductionPlan.builder()
                .id(null).product(mockProduct).kitchen(mockKitchen).build();
        Page<ProductionPlan> planPage = new PageImpl<>(List.of(plan));
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(planPage);

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, mockPrincipal);

        assertNotNull(result);
        // ingredients sẽ là null vì plan.getId() == null
        assertNull(result.getContent().get(0).getIngredients());
        // Không gọi planIngredientRepository vì skip
        verify(planIngredientRepository, never()).findByPlan_Id(any());
    }

    // ==================== CREATE PRODUCTION PLAN TESTS ====================

    @Test
    void createProductionPlan_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("30.0")).build();
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc(eq("KIT001"), eq("ING001")))
                .thenReturn(List.of(batch));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArgument(0));

        ProductionPlanResponse result = centralKitchenService.createProductionPlan(request, mockPrincipal);
        assertNotNull(result);
    }

    /**
     * FIX 2.2: createProductionPlan với nhiều recipe (nhiều nguyên liệu).
     */
    @Test
    void createProductionPlan_MultipleRecipes_AllIngredientsSufficient_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(5)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Ingredient ing2 = Ingredient.builder().id("ING002").name("Muối").unit("g").build();

        Recipe recipe1 = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")).unit("kg").build();
        Recipe recipe2 = Recipe.builder()
                .product(mockProduct).ingredient(ing2)
                .quantity(new BigDecimal("100.0")).unit("g").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe1, recipe2));

        IngredientBatch batchIng1 = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("20.0")).build();
        IngredientBatch batchIng2 = IngredientBatch.builder()
                .id("B002").ingredient(ing2)
                .remainingQuantity(new BigDecimal("1000.0")).build();

        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc("KIT001", "ING001"))
                .thenReturn(List.of(batchIng1));
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc("KIT001", "ING002"))
                .thenReturn(List.of(batchIng2));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArgument(0));

        ProductionPlanResponse result = centralKitchenService.createProductionPlan(request, mockPrincipal);

        assertNotNull(result);
        // Verify 2 PlanIngredient được lưu
        verify(planIngredientRepository).saveAll(argThat(pis -> {
            List<PlanIngredient> list = (List<PlanIngredient>) pis;
            return list.size() == 2;
        }));
    }

    @Test
    void createProductionPlan_NoKitchen_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(CreateProductionPlanRequest.builder().build(), mockPrincipal));
    }

    @Test
    void createProductionPlan_InvalidDateRange_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .startDate(LocalDateTime.now().plusDays(1)).endDate(LocalDateTime.now()).build();

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));
    }

    @Test
    void createProductionPlan_ProductNotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD999")).thenReturn(Optional.empty());

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD999").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));
    }

    @Test
    void createProductionPlan_NoRecipe_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(new ArrayList<>());

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));
    }

    @Test
    void createProductionPlan_NotEnoughIngredient_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("100.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("10.0")).build();
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc(eq("KIT001"), eq("ING001")))
                .thenReturn(List.of(batch));

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));
    }

    /**
     * FIX 2.1: quantity = 0 → requiredTotal = 0 → không lấy nguyên liệu, không có usage.
     * Recipe tồn tại nhưng không cần nguyên liệu nào cả.
     */
    @Test
    void createProductionPlan_QuantityZero_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(new ArrayList<>());

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(0)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));
    }

    // ==================== START PRODUCTION PLAN TESTS ====================

    @Test
    void startProductionPlan_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("20.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch).quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("20.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.startProductionPlan("PLAN001", mockPrincipal);
        assertNotNull(result);
    }

    /**
     * FIX 3.2: startProductionPlan — batch dùng hết (newQty == 0) → set status "DEPLETED".
     */
    @Test
    void startProductionPlan_BatchExactlyDepleted_SetStatusDepleted() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        // Batch còn đúng 10kg, dùng hết 10kg → DEPLETED
        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("10.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch).quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("10.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        centralKitchenService.startProductionPlan("PLAN001", mockPrincipal);

        assertEquals(BigDecimal.ZERO, batch.getRemainingQuantity().stripTrailingZeros());
        assertEquals("DEPLETED", batch.getStatus());
    }

    /**
     * FIX 3.3: startProductionPlan — KitchenInventory không tồn tại → bỏ qua cập nhật inventory.
     */
    @Test
    void startProductionPlan_NoInventoryRecord_SkipsInventoryUpdate() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("20.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch).quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        // Inventory không tồn tại → nhánh inv == null
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.empty());

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.startProductionPlan("PLAN001", mockPrincipal);

        assertNotNull(result);
        assertEquals("IN_PRODUCTION", plan.getStatus());
        // Inventory không được cập nhật
        verify(kitchenInventoryRepository, never()).save(any(KitchenInventory.class));
    }

    /**
     * FIX 3.1: startProductionPlan — batch không đủ số lượng so với lúc tạo plan → exception.
     */
    @Test
    void startProductionPlan_BatchInsufficientAfterPlanCreated_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        // Batch chỉ còn 5kg nhưng usage yêu cầu 10kg
        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient).batchNo("BN001")
                .remainingQuantity(new BigDecimal("5.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch).quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                centralKitchenService.startProductionPlan("PLAN001", mockPrincipal));

        assertTrue(ex.getMessage().contains("does not have enough qty now"));
    }

    @Test
    void startProductionPlan_WrongStatus_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("COMPLETED").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.startProductionPlan("PLAN001", mockPrincipal));
    }

    @Test
    void startProductionPlan_PlanNotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLAN999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.startProductionPlan("PLAN999", mockPrincipal));
    }

    // ==================== COMPLETE PRODUCTION PLAN TESTS ====================

    @Test
    void completeProductionPlan_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct)
                .quantity(50).unit("ổ").status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.completeProductionPlan(
                "PLAN001", "Completed", LocalDate.now().plusMonths(1), mockPrincipal);

        assertNotNull(result);
        assertEquals("COMPLETED", plan.getStatus());
    }

    /**
     * FIX 4.1: completeProductionPlan — notes không null → gọi appendInternalNote.
     */
    @Test
    void completeProductionPlan_WithNotes_AppendsNoteToplan() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct)
                .quantity(10).unit("ổ").status("IN_PRODUCTION").notes(null).build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        centralKitchenService.completeProductionPlan(
                "PLAN001", "Hoàn thành xuất sắc", LocalDate.now().plusMonths(1), mockPrincipal);

        assertNotNull(plan.getNotes());
        assertTrue(plan.getNotes().contains("Hoàn thành xuất sắc"));
    }

    @Test
    void completeProductionPlan_WrongStatus_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.completeProductionPlan("PLAN001", null, LocalDate.now(), mockPrincipal));
    }

    @Test
    void completeProductionPlan_NoExpiryDate_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.completeProductionPlan("PLAN001", null, null, mockPrincipal));
    }

    @Test
    void completeProductionPlan_PlanNotFound_ThrowsNotFoundException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLN_404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.completeProductionPlan(
                        "PLN_404", "done", LocalDate.now().plusMonths(1), mockPrincipal));
    }

    @Test
    void completeProductionPlan_ExpiryDateInPast_StillSucceeds() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_PAST").kitchen(mockKitchen).product(mockProduct)
                .quantity(10).unit("ổ").status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLN_PAST")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLN_PAST")).thenReturn(new ArrayList<>());

        // Service không validate ngày quá khứ, chỉ validate null
        ProductionPlanResponse result = centralKitchenService.completeProductionPlan(
                "PLN_PAST", null, LocalDate.now().minusDays(1), mockPrincipal);

        assertNotNull(result);
        assertEquals("COMPLETED", plan.getStatus());
    }

    // ==================== CANCEL PRODUCTION PLAN TESTS ====================

    @Test
    void cancelProductionPlan_Success_DraftPlan() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.cancelProductionPlan("PLAN001", "Cancel", mockPrincipal);
        assertNotNull(result);
        assertEquals("CANCELLED", plan.getStatus());
    }

    @Test
    void cancelProductionPlan_CompletedPlan_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("COMPLETED").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.cancelProductionPlan("PLAN001", null, mockPrincipal));
    }

    /**
     * FIX 5.1: cancelProductionPlan IN_PRODUCTION — batch DEPLETED được restore về ACTIVE.
     */
    @Test
    void cancelProductionPlan_InProduction_DepletedBatchRestoredToActive() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        // Batch đang DEPLETED (đã dùng hết khi start), restore lại 10kg → chuyển về ACTIVE
        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .status("DEPLETED").remainingQuantity(BigDecimal.ZERO).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(batch)
                .quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(BigDecimal.ZERO).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        centralKitchenService.cancelProductionPlan("PLAN001", null, mockPrincipal);

        // DEPLETED → ACTIVE sau khi restore
        assertEquals("ACTIVE", batch.getStatus());
        assertEquals(new BigDecimal("10.0"), batch.getRemainingQuantity());
    }

    /**
     * FIX 5.2: cancelProductionPlan IN_PRODUCTION — KitchenInventory không tồn tại → bỏ qua.
     */
    @Test
    void cancelProductionPlan_InProduction_NoInventoryRecord_SkipsInventoryRestore() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct).status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .status("ACTIVE").remainingQuantity(new BigDecimal("5.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(batch)
                .quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        // Không có KitchenInventory → nhánh inv == null
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.empty());

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.cancelProductionPlan("PLAN001", null, mockPrincipal);

        assertNotNull(result);
        assertEquals("CANCELLED", plan.getStatus());
        verify(kitchenInventoryRepository, never()).save(any(KitchenInventory.class));
    }

    /**
     * FIX 5.3: cancelProductionPlan — notes không null → gọi appendInternalNote.
     */
    @Test
    void cancelProductionPlan_WithNotes_AppendsNoteToplan() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN001").kitchen(mockKitchen).product(mockProduct)
                .status("DRAFT").notes(null).build();
        when(productionPlanRepository.findById("PLAN001")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        centralKitchenService.cancelProductionPlan("PLAN001", "Lý do huỷ kế hoạch", mockPrincipal);

        assertNotNull(plan.getNotes());
        assertTrue(plan.getNotes().contains("Lý do huỷ kế hoạch"));
    }

    @Test
    void cancelProductionPlan_PlanNotFound_ThrowsNotFoundException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productionPlanRepository.findById("PLN_MISSING")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.cancelProductionPlan("PLN_MISSING", "reason", mockPrincipal));
    }

    @Test
    void cancelProductionPlan_InProgressPlan_SuccessWithIngredientRestore() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_INPROD").kitchen(mockKitchen).product(mockProduct).status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLN_INPROD")).thenReturn(Optional.of(plan));

        IngredientBatch ingBatch = IngredientBatch.builder()
                .id("IB001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("5.0")).status("ACTIVE").build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(ingBatch).quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLN_INPROD")).thenReturn(List.of(usage));

        KitchenInventory inv = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("5.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inv));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLN_INPROD")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.cancelProductionPlan(
                "PLN_INPROD", "cancel reason", mockPrincipal);

        assertNotNull(result);
        assertEquals("CANCELLED", plan.getStatus());
        verify(ingredientBatchRepository).save(ingBatch);
        verify(kitchenInventoryRepository).save(inv);
        verify(planIngredientBatchUsageRepository).deleteAll(anyList());
    }

    // ==================== INVENTORY TESTS ====================

    @Test
    void getInventory_DelegatesToIngredientBatchService() {
        Page<KitchenInventoryDetailResponse> expectedPage = new PageImpl<>(new ArrayList<>());
        when(ingredientBatchService.getInventory("ING001", "Bột", false, 0, 20, mockPrincipal))
                .thenReturn(expectedPage);

        Page<KitchenInventoryDetailResponse> result = centralKitchenService.getInventory(
                "ING001", "Bột", false, 0, 20, mockPrincipal);

        assertSame(expectedPage, result);
        verify(ingredientBatchService).getInventory("ING001", "Bột", false, 0, 20, mockPrincipal);
    }

    // ==================== PRODUCT TESTS ====================

    @Test
    void getProducts_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Product> productPage = new PageImpl<>(List.of(mockProduct));
        when(productRepository.searchProducts(any(), any(), any(PageRequest.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().build());

        Page<ProductResponse> result = centralKitchenService.getProducts("Bánh", "BAKERY", 0, 20, mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void getProducts_InvalidCategory_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProducts("Bánh", "INVALID", 0, 20, mockPrincipal));
    }

    // ==================== STORE TESTS ====================

    @Test
    void getStores_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Store> storePage = new PageImpl<>(List.of(mockStore));
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(storePage);

        Page<StoreResponse> result = centralKitchenService.getStores("District", "ACTIVE", 0, 20, mockPrincipal);
        assertNotNull(result);
    }

    // ==================== KITCHEN TESTS ====================

    @Test
    void getMyKitchen_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        KitchenResponse result = centralKitchenService.getMyKitchen(mockPrincipal);
        assertNotNull(result);
        assertEquals("KIT001", result.getId());
    }

    @Test
    void getMyKitchen_NoKitchenAssigned_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getMyKitchen(mockPrincipal));
    }

    // ==================== OVERVIEW TESTS ====================

    @Test
    void getOverview_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(null, null, mockPrincipal);
        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
    }

    @Test
    void getOverview_NoKitchen_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getOverview(null, null, mockPrincipal));
    }

    @Test
    void getOverview_InvalidDateRange_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getOverview(
                        LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 1), mockPrincipal));
    }

    @Test
    void getOverview_WithValidDateRange_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(5L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), mockPrincipal);

        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
    }

    @Test
    void getOverview_SameDateRange_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);

        LocalDate sameDay = LocalDate.of(2026, 4, 15);
        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(sameDay, sameDay, mockPrincipal);
        assertNotNull(result);
    }

    /**
     * FIX 8.2: getOverview — chỉ có fromDate, toDate null.
     */
    @Test
    void getOverview_OnlyFromDate_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(2L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                LocalDate.of(2026, 4, 1), null, mockPrincipal);

        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
    }

    /**
     * FIX 8.2: getOverview — chỉ có toDate, fromDate null.
     */
    @Test
    void getOverview_OnlyToDate_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(3L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                null, LocalDate.of(2026, 4, 30), mockPrincipal);

        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
    }

    // ==================== PRODUCT BATCH TESTS ====================

    @Test
    void getProductBatches_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen).build();
        Page<Batch> batchPage = new PageImpl<>(List.of(batch));
        when(batchRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(batchPage);
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        Page<BatchResponse> result = centralKitchenService.getProductBatches("PROD001", "AVAILABLE", 0, 20, mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void getProductBatches_NoKitchen_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProductBatches("PROD001", "AVAILABLE", 0, 20, mockPrincipal));
    }

    @Test
    void getProductBatches_NoKitchen_ThrowsExceptionForNullProductIdToo() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProductBatches(null, null, 0, 20, mockPrincipal));
    }

    @Test
    void getProductBatches_WithStatusFilter_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH_AVAIL").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(200).status("AVAILABLE").build();
        Page<Batch> batchPage = new PageImpl<>(List.of(batch));
        when(batchRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(batchPage);
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        Page<BatchResponse> result = centralKitchenService.getProductBatches(null, "AVAILABLE", 0, 20, mockPrincipal);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductBatchById_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));

        BatchResponse result = centralKitchenService.getProductBatchById("BATCH001", mockPrincipal);
        assertNotNull(result);
        // batch.getPlan() == null → usages = null (nhánh else của toBatchResponse)
        assertNull(result.getIngredientBatchUsages());
    }

    @Test
    void getProductBatchById_NotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(batchRepository.findById("BATCH999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.getProductBatchById("BATCH999", mockPrincipal));
    }

    @Test
    void getProductBatchById_WrongKitchen_ThrowsException() {
        Kitchen otherKitchen = Kitchen.builder().id("KIT002").build();
        mockStaff.setKitchen(otherKitchen);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProductBatchById("BATCH001", mockPrincipal));
    }

    /**
     * FIX 11.1: toBatchResponse — batch có plan → gọi planIngredientBatchUsageRepository.
     */
    @Test
    void getProductBatchById_WithPlan_ReturnsUsages() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder().id("PLAN001").build();
        Batch batch = Batch.builder()
                .id("BATCH001").plan(plan).product(mockProduct).kitchen(mockKitchen).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(new ArrayList<>());

        BatchResponse result = centralKitchenService.getProductBatchById("BATCH001", mockPrincipal);

        assertNotNull(result);
        verify(planIngredientBatchUsageRepository).findByPlan_Id("PLAN001");
    }

    @Test
    void updateBatch_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        UpdateBatchRequest request = new UpdateBatchRequest();
        BatchResponse result = centralKitchenService.updateBatch("BATCH001", request, mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void updateBatch_NotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(batchRepository.findById("BATCH999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.updateBatch("BATCH999", new UpdateBatchRequest(), mockPrincipal));
    }

    @Test
    void updateBatch_WrongKitchen_StillSucceeds() {
        Kitchen otherKitchen = Kitchen.builder().id("KIT999").build();
        mockStaff.setKitchen(otherKitchen);

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH_WRONG").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH_WRONG")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        BatchResponse result = centralKitchenService.updateBatch("BATCH_WRONG", new UpdateBatchRequest(), mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void updateBatch_NoKitchenOnStaff_StillSucceeds() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        BatchResponse result = centralKitchenService.updateBatch("BATCH001", new UpdateBatchRequest(), mockPrincipal);
        assertNotNull(result);
    }

    // ==================== PRODUCT INVENTORY TESTS ====================

    @Test
    void getProductInventory_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(batchRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>());

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 0, 20, mockPrincipal);
        assertNotNull(result);
    }

    @Test
    void getProductInventory_NoKitchen_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getProductInventory("PROD001", "Bánh", 0, 20, mockPrincipal));
    }

    @Test
    void getProductInventory_WithProductIdFilter_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(batch));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                "PROD001", null, 0, 20, mockPrincipal);

        assertNotNull(result);
        verify(batchRepository).findAll(any(Specification.class));
    }

    @Test
    void getProductInventory_WithProductNameFilter_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(batch));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, "Bánh", 0, 20, mockPrincipal);

        assertNotNull(result);
    }

    @Test
    void getProductInventory_WithBothFilters_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(batch));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                "PROD001", "Bánh", 0, 20, mockPrincipal);

        assertNotNull(result);
    }

    /**
     * FIX 7.1: getProductInventory — manual pagination: kết quả nhiều hơn page size,
     * page = 1 → subList phải trả về trang thứ 2.
     */
    @Test
    void getProductInventory_Pagination_SecondPage_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        // Tạo 3 product khác nhau, mỗi product 1 batch
        Product prod2 = Product.builder().id("PROD002").name("Bánh Bao").unit("cái").build();
        Product prod3 = Product.builder().id("PROD003").name("Bánh Cuốn").unit("phần").build();

        Batch b1 = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        Batch b2 = Batch.builder().id("B2").product(prod2).kitchen(mockKitchen)
                .remainingQuantity(20).status("AVAILABLE").build();
        Batch b3 = Batch.builder().id("B3").product(prod3).kitchen(mockKitchen)
                .remainingQuantity(30).status("AVAILABLE").build();

        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(b1, b2, b3));

        // page=1, size=2 → trang thứ 2, chỉ có 1 phần tử
        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 1, 2, mockPrincipal);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements()); // Tổng 3 sản phẩm
        assertEquals(1, result.getContent().size()); // Trang 2 chỉ có 1 phần tử
    }

    /**
     * FIX 7.1: getProductInventory — page offset vượt quá size thực tế → subList trả về rỗng.
     */
    @Test
    void getProductInventory_Pagination_OffsetBeyondResults_ReturnsEmpty() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch b1 = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();

        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(b1));

        // page=5, size=20 → offset = 100 > 1 → subList rỗng
        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 5, 20, mockPrincipal);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    // ==================== RECIPE CHECK TESTS ====================

    @Test
    void checkRecipeAvailability_Success_CanProduce() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("30.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        RecipeCheckResponse result = centralKitchenService.checkRecipeAvailability("PROD001", 10, mockPrincipal);

        assertNotNull(result);
        assertTrue(result.isCanProduce());
    }

    @Test
    void checkRecipeAvailability_NoKitchen_ThrowsException() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.checkRecipeAvailability("PROD001", 10, mockPrincipal));
    }

    @Test
    void checkRecipeAvailability_ProductNotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.checkRecipeAvailability("PROD999", 10, mockPrincipal));
    }

    @Test
    void checkRecipeAvailability_NoRecipe_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(new ArrayList<>());

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.checkRecipeAvailability("PROD001", 10, mockPrincipal));
    }

    @Test
    void checkRecipeAvailability_WithShortage_SetsShortageCorrectly() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("10.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        RecipeCheckResponse result = centralKitchenService.checkRecipeAvailability("PROD001", 10, mockPrincipal);

        assertNotNull(result);
        assertFalse(result.isCanProduce());
        assertEquals(new BigDecimal("10.0"), result.getIngredients().get(0).getShortage());
    }

    @Test
    void checkRecipeAvailability_NoInventoryForIngredient_TreatedAsZero() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("1.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.empty());

        RecipeCheckResponse result = centralKitchenService.checkRecipeAvailability("PROD001", 5, mockPrincipal);

        assertNotNull(result);
        assertFalse(result.isCanProduce());
        assertEquals(new BigDecimal("5.0"), result.getIngredients().get(0).getShortage());
    }

    @Test
    void checkRecipeAvailability_ExactlyEnoughIngredient_CanProduce() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("2.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        KitchenInventory inventory = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("10.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inventory));

        // 5 sản phẩm × 2kg = 10kg đúng bằng available
        RecipeCheckResponse result = centralKitchenService.checkRecipeAvailability("PROD001", 5, mockPrincipal);

        assertNotNull(result);
        assertTrue(result.isCanProduce());
    }

    // ==================== HANDLE PRODUCT DEDUCTION ON PACKING TESTS ====================

    @Test
    void handleProductDeductionOnPacking_WithValidStock_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        OrderItem orderItem = OrderItem.builder()
                .id(1).product(mockProduct).quantity(50).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);

        assertNotNull(result);
        verify(batchRepository, atLeastOnce()).save(any(Batch.class));
    }

    @Test
    void handleProductDeductionOnPacking_NotEnoughStock_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        OrderItem orderItem = OrderItem.builder()
                .id(1).product(mockProduct).quantity(100).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        assertThrows(BadRequestException.class, () ->
                centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal));
    }

    @Test
    void handleProductDeductionOnPacking_MultipleBatchesFEFO_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        OrderItem orderItem = OrderItem.builder()
                .id(1).product(mockProduct).quantity(60).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        Batch batch1 = Batch.builder().id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        Batch batch2 = Batch.builder().id("BATCH002").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(30).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch1, batch2));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);

        assertNotNull(result);
        // batch1 dùng hết 50 → DISTRIBUTED
        assertEquals("DISTRIBUTED", batch1.getStatus());
        assertEquals(0, batch1.getRemainingQuantity());
        // batch2 chỉ dùng 10 → PARTIALLY_DISTRIBUTED
        assertEquals("PART_DIST", batch2.getStatus());
        assertEquals(20, batch2.getRemainingQuantity());
    }

    /**
     * FIX 9.2: handleProductDeductionOnPacking — batch lớn hơn nhu cầu → PARTIALLY_DISTRIBUTED.
     */
    @Test
    void handleProductDeductionOnPacking_SingleBatch_PartiallyDistributed() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        OrderItem orderItem = OrderItem.builder()
                .id(1).product(mockProduct).quantity(30).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        // Batch có 100, chỉ dùng 30 → PARTIALLY_DISTRIBUTED
        Batch batch = Batch.builder()
                .id("BATCH001").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);

        assertEquals("PART_DIST", batch.getStatus());
        assertEquals(70, batch.getRemainingQuantity());
    }

    /**
     * FIX 9.3: handleProductDeductionOnPacking — nhiều OrderItem (nhiều sản phẩm khác nhau).
     */
    @Test
    void handleProductDeductionOnPacking_MultipleOrderItems_DeductsEach() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Product prod2 = Product.builder().id("PROD002").name("Bánh Bao").unit("cái").build();

        OrderItem item1 = OrderItem.builder().id(1).product(mockProduct).quantity(10).build();
        OrderItem item2 = OrderItem.builder().id(2).product(prod2).quantity(20).build();
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(item1, item2));

        Batch batchProd1 = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        Batch batchProd2 = Batch.builder().id("B2").product(prod2).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();

        // Mỗi lần gọi findAll trả về batch tương ứng
        when(batchRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(batchProd1))
                .thenReturn(List.of(batchProd2));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD001", request, mockPrincipal);

        assertNotNull(result);
        // Cả 2 batch đều được cập nhật
        verify(batchRepository, times(2)).save(any(Batch.class));
        assertEquals(40, batchProd1.getRemainingQuantity());
        assertEquals(30, batchProd2.getRemainingQuantity());
    }

    // ==================== MARK ORDER STATUS TIMESTAMP TESTS ====================

    @Test
    void updateOrderStatus_ToAssigned_SetsAssignedTimestamp() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order pendingOrder = Order.builder()
                .id("ORD_ASSIGN").store(mockStore).status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD_ASSIGN")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_ASSIGN")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.ASSIGNED);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_ASSIGN", request, mockPrincipal);

        assertNotNull(pendingOrder.getAssignedAt());
    }

    /**
     * FIX 12.2: markOrderStatusTimestamp — assignedAt đã có → không set lại.
     */
    @Test
    void updateOrderStatus_ToAssigned_AlreadyHasAssignedAt_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingAssignedAt = LocalDateTime.now().minusHours(2);
        Order pendingOrder = Order.builder()
                .id("ORD_ALREADY").store(mockStore)
                .status(OrderStatus.PENDING)
                .assignedAt(existingAssignedAt) // đã có timestamp
                .build();
        when(orderRepository.findById("ORD_ALREADY")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_ALREADY")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.ASSIGNED);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_ALREADY", request, mockPrincipal);

        // assignedAt không bị ghi đè
        assertEquals(existingAssignedAt, pendingOrder.getAssignedAt());
    }

    @Test
    void updateOrderStatus_ToInProgress_FromPending_SetsBothTimestamps() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order pendingOrder = Order.builder()
                .id("ORD_INPROG").store(mockStore).status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD_INPROG")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_INPROG")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_INPROG", request, mockPrincipal);

        assertNotNull(pendingOrder.getAssignedAt());
        assertNotNull(pendingOrder.getInProgressAt());
    }

    @Test
    void updateOrderStatus_ToShipping_FromPacked_SetsShippingTimestamp() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order packedOrder = Order.builder()
                .id("ORD_SHIP").store(mockStore).status(OrderStatus.PACKED_WAITING_SHIPPER).build();
        when(orderRepository.findById("ORD_SHIP")).thenReturn(Optional.of(packedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(packedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_SHIP")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.SHIPPING);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_SHIP", request, mockPrincipal);

        assertNotNull(packedOrder.getShippingAt());
    }

    @Test
    void updateOrderStatus_ToDelivered_FromShipping_SetsDeliveredTimestamp() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order shippingOrder = Order.builder()
                .id("ORD_DELIVER").store(mockStore).status(OrderStatus.SHIPPING).priority("HIGH").build();
        when(orderRepository.findById("ORD_DELIVER")).thenReturn(Optional.of(shippingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(shippingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_DELIVER")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.DELIVERED);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_DELIVER", request, mockPrincipal);

        assertNotNull(shippingOrder.getDeliveredAt());
    }

    @Test
    void updateOrderStatus_ToCancelled_FromAssigned_SetsCancelledTimestamp() {
        Order assignedOrder = Order.builder()
                .id("ORD_CANCEL").store(mockStore).status(OrderStatus.ASSIGNED).priority("NORMAL").build();

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_CANCEL")).thenReturn(Optional.of(assignedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(assignedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_CANCEL")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(request.getNotes()).thenReturn("Khách huỷ");

        centralKitchenService.updateOrderStatus("ORD_CANCEL", request, mockPrincipal);

        assertNotNull(assignedOrder.getCancelledAt());
    }

    /**
     * FIX 12.2: DELIVERED — các timestamp trước đó đã có sẵn → không ghi đè.
     */
    @Test
    void updateOrderStatus_ToDelivered_AllTimestampsAlreadySet_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingAssigned = LocalDateTime.now().minusHours(5);
        LocalDateTime existingInProgress = LocalDateTime.now().minusHours(4);
        LocalDateTime existingPacked = LocalDateTime.now().minusHours(3);
        LocalDateTime existingShipping = LocalDateTime.now().minusHours(2);

        Order shippingOrder = Order.builder()
                .id("ORD_DELIVER_TS").store(mockStore).status(OrderStatus.SHIPPING)
                .assignedAt(existingAssigned)
                .inProgressAt(existingInProgress)
                .packedWaitingShipperAt(existingPacked)
                .shippingAt(existingShipping)
                .build();
        when(orderRepository.findById("ORD_DELIVER_TS")).thenReturn(Optional.of(shippingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(shippingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_DELIVER_TS")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.DELIVERED);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_DELIVER_TS", request, mockPrincipal);

        // Các timestamp cũ không bị ghi đè
        assertEquals(existingAssigned, shippingOrder.getAssignedAt());
        assertEquals(existingInProgress, shippingOrder.getInProgressAt());
        assertEquals(existingPacked, shippingOrder.getPackedWaitingShipperAt());
        assertEquals(existingShipping, shippingOrder.getShippingAt());
        assertNotNull(shippingOrder.getDeliveredAt());
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    void nullPrincipal_ThrowsException() {
        assertThrows(IllegalStateException.class, () ->
                centralKitchenService.getMyKitchen(null));
    }

    @Test
    void userNotFound_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                centralKitchenService.getMyKitchen(mockPrincipal));
    }

    @Test
    void nonStaffUser_ThrowsException() {
        Role nonStaffRole = Role.builder().name("STORE_STAFF").build();
        mockStaff.setRole(nonStaffRole);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        assertThrows(IllegalStateException.class, () ->
                centralKitchenService.getMyKitchen(mockPrincipal));
    }

    // ==================== MISC TESTS ====================

    @Test
    void getOrderById_OrderBelongsToDifferentKitchen_StillReturns() {
        Kitchen otherKitchen = Kitchen.builder().id("KIT099").build();
        Order orderInOtherKitchen = Order.builder()
                .id("ORD_OTHER").store(mockStore).kitchen(otherKitchen)
                .status(OrderStatus.ASSIGNED).priority("NORMAL")
                .total(new BigDecimal("50000")).build();

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.findById("ORD_OTHER")).thenReturn(Optional.of(orderInOtherKitchen));
        when(orderItemRepository.findByOrder_Id("ORD_OTHER")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> {
            OrderResponse result = centralKitchenService.getOrderById("ORD_OTHER", mockPrincipal);
            assertNotNull(result);
        });
    }

    @Test
    void startProductionPlan_DifferentKitchen_StillProcesses() {
        Kitchen otherKitchen = Kitchen.builder().id("KIT002").build();
        mockStaff.setKitchen(otherKitchen);

        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_WRONG_KIT").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLN_WRONG_KIT")).thenReturn(Optional.of(plan));
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLN_WRONG_KIT")).thenReturn(new ArrayList<>());
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLN_WRONG_KIT")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.startProductionPlan("PLN_WRONG_KIT", mockPrincipal);

        assertNotNull(result);
        assertEquals("IN_PRODUCTION", plan.getStatus());
    }
    // ==================== NEW TESTS FOR REMAINING COVERAGE ====================

    // ===== FIX 1 (209-210): updateOrderStatus → handleProductDeductionOnPacking =====

    /**
     * updateOrderStatus chuyển sang PACKED_WAITING_SHIPPER từ IN_PROGRESS
     * → gọi handleProductDeductionOnPacking thực sự.
     */
    @Test
    void updateOrderStatus_ToPackedWaitingShipper_FromInProgress_CallsProductDeduction() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order inProgressOrder = Order.builder()
                .id("ORD_PACK").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_PACK")).thenReturn(Optional.of(inProgressOrder));

        OrderItem item = OrderItem.builder()
                .id(1).product(mockProduct).quantity(10).build();
        when(orderItemRepository.findByOrder_Id("ORD_PACK")).thenReturn(List.of(item));

        Batch batch = Batch.builder()
                .id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD_PACK", request, mockPrincipal);

        assertNotNull(result);
        assertEquals(OrderStatus.PACKED_WAITING_SHIPPER, inProgressOrder.getStatus());
        assertNotNull(inProgressOrder.getPackedWaitingShipperAt());
        verify(batchRepository, atLeastOnce()).save(any(Batch.class));
        assertEquals(40, batch.getRemainingQuantity());
    }

    // ===== FIX 2 (177-179): assignOrder - assignedAt đã có giá trị =====

    /**
     * assignOrder khi order.getAssignedAt() đã có giá trị → không ghi đè.
     */
    @Test
    void assignOrder_AssignedAtAlreadySet_DoesNotOverwrite() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingAssignedAt = LocalDateTime.now().minusHours(3);
        Order orderWithAssignedAt = Order.builder()
                .id("ORD_ASSIGNED").store(mockStore)
                .status(OrderStatus.ASSIGNED)
                .assignedAt(existingAssignedAt)
                .build();
        when(orderRepository.findById("ORD_ASSIGNED")).thenReturn(Optional.of(orderWithAssignedAt));
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithAssignedAt);
        when(orderItemRepository.findByOrder_Id("ORD_ASSIGNED")).thenReturn(new ArrayList<>());

        OrderResponse result = centralKitchenService.assignOrder("ORD_ASSIGNED", mockPrincipal);

        assertNotNull(result);
        assertEquals(existingAssignedAt, orderWithAssignedAt.getAssignedAt());
    }

    // ===== FIX 3: getProductionPlanById - currentUser.getKitchen() == null =====

    /**
     * getProductionPlanById khi currentUser.getKitchen() == null
     * → bỏ qua kiểm tra kitchen ownership, trả về plan bình thường.
     */
    @Test
    void getProductionPlanById_StaffHasNoKitchen_ReturnsAnyPlan() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN001")).thenReturn(Optional.of(plan));
        when(planIngredientRepository.findByPlan_Id("PLN001")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.getProductionPlanById("PLN001", mockPrincipal);

        assertNotNull(result);
        assertEquals("PLN001", result.getId());
    }

    // ===== FIX 4: startProductionPlan - full flow =====

    /**
     * startProductionPlan - full flow với nhiều usages, inventory update.
     */
    @Test
    void startProductionPlan_FullFlow_DeductsIngredientsAndUpdatesInventory() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_FULL").kitchen(mockKitchen).product(mockProduct).status("DRAFT").build();
        when(productionPlanRepository.findById("PLAN_FULL")).thenReturn(Optional.of(plan));

        Ingredient ing1 = Ingredient.builder().id("ING001").name("Bột").build();
        Ingredient ing2 = Ingredient.builder().id("ING002").name("Muối").build();

        IngredientBatch batch1 = IngredientBatch.builder()
                .id("IB1").ingredient(ing1).remainingQuantity(new BigDecimal("20.0")).build();
        IngredientBatch batch2 = IngredientBatch.builder()
                .id("IB2").ingredient(ing2).remainingQuantity(new BigDecimal("10.0")).build();

        PlanIngredientBatchUsage usage1 = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch1).quantityUsed(new BigDecimal("15.0")).build();
        PlanIngredientBatchUsage usage2 = PlanIngredientBatchUsage.builder()
                .ingredientBatch(batch2).quantityUsed(new BigDecimal("5.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN_FULL"))
                .thenReturn(List.of(usage1, usage2));

        KitchenInventory inv1 = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(ing1).totalQuantity(new BigDecimal("20.0")).build();
        KitchenInventory inv2 = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(ing2).totalQuantity(new BigDecimal("10.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inv1));
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING002"))
                .thenReturn(Optional.of(inv2));

        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN_FULL")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.startProductionPlan("PLAN_FULL", mockPrincipal);

        assertNotNull(result);
        assertEquals("IN_PRODUCTION", plan.getStatus());

        assertEquals(new BigDecimal("5.0"), batch1.getRemainingQuantity());
        assertEquals(new BigDecimal("5.0"), batch2.getRemainingQuantity());

        assertEquals(new BigDecimal("5.0"), inv1.getTotalQuantity());
        assertEquals(new BigDecimal("5.0"), inv2.getTotalQuantity());

        verify(ingredientBatchRepository, times(2)).save(any(IngredientBatch.class));
        verify(kitchenInventoryRepository, times(2)).save(any(KitchenInventory.class));
    }

    /**
     * startProductionPlan với status APPROVED (legacy).
     */
    @Test
    void startProductionPlan_FromApprovedStatus_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_APPR").kitchen(mockKitchen).product(mockProduct).status("APPROVED").build();
        when(productionPlanRepository.findById("PLAN_APPR")).thenReturn(Optional.of(plan));
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN_APPR")).thenReturn(new ArrayList<>());
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenReturn(plan);
        when(planIngredientRepository.findByPlan_Id("PLAN_APPR")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.startProductionPlan("PLAN_APPR", mockPrincipal);

        assertNotNull(result);
        assertEquals("IN_PRODUCTION", plan.getStatus());
    }

    // ===== FIX 5 (390-392): completeProductionPlan - notes variations =====

    /**
     * completeProductionPlan với notes có giá trị (oldNotes có sẵn).
     */
    @Test
    void completeProductionPlan_WithNonBlankNotes_OldNotesExists_AppendsNotes() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_NOTES").kitchen(mockKitchen).product(mockProduct)
                .quantity(10).unit("ổ").status("IN_PRODUCTION")
                .notes("Existing notes").build();
        when(productionPlanRepository.findById("PLAN_NOTES")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN_NOTES")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.completeProductionPlan(
                "PLAN_NOTES", "Completion notes", LocalDate.now().plusMonths(1), mockPrincipal);

        assertNotNull(result);
        assertTrue(plan.getNotes().contains("Existing notes"));
        assertTrue(plan.getNotes().contains("Completion notes"));
    }

    /**
     * completeProductionPlan với notes = null → không append.
     */
    @Test
    void completeProductionPlan_WithNullNotes_DoesNotAppend() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_NULL").kitchen(mockKitchen).product(mockProduct)
                .quantity(10).unit("ổ").status("IN_PRODUCTION")
                .notes("Original").build();
        when(productionPlanRepository.findById("PLAN_NULL")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN_NULL")).thenReturn(new ArrayList<>());

        centralKitchenService.completeProductionPlan(
                "PLAN_NULL", null, LocalDate.now().plusMonths(1), mockPrincipal);

        assertEquals("Original", plan.getNotes());
    }

    /**
     * completeProductionPlan với notes = "  " (blank) → không append.
     */
    @Test
    void completeProductionPlan_WithBlankNotes_DoesNotAppend() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_BLANK").kitchen(mockKitchen).product(mockProduct)
                .quantity(10).unit("ổ").status("IN_PRODUCTION")
                .notes("Original").build();
        when(productionPlanRepository.findById("PLAN_BLANK")).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN_BLANK")).thenReturn(new ArrayList<>());

        centralKitchenService.completeProductionPlan(
                "PLAN_BLANK", "   ", LocalDate.now().plusMonths(1), mockPrincipal);

        assertEquals("Original", plan.getNotes());
    }

    // ===== FIX 6: cancelProductionPlan - full IN_PRODUCTION flow =====

    /**
     * cancelProductionPlan từ IN_PRODUCTION - full flow restore.
     */
    @Test
    void cancelProductionPlan_FromInProduction_FullRestoreFlow() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_CANCEL").kitchen(mockKitchen).product(mockProduct)
                .status("IN_PRODUCTION").notes("Old notes").build();
        when(productionPlanRepository.findById("PLAN_CANCEL")).thenReturn(Optional.of(plan));

        IngredientBatch depletedBatch = IngredientBatch.builder()
                .id("IB_DEP").ingredient(mockIngredient)
                .status("DEPLETED").remainingQuantity(BigDecimal.ZERO).build();
        IngredientBatch activeBatch = IngredientBatch.builder()
                .id("IB_ACT").ingredient(mockIngredient)
                .status("ACTIVE").remainingQuantity(new BigDecimal("5.0")).build();

        PlanIngredientBatchUsage usage1 = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(depletedBatch)
                .quantityUsed(new BigDecimal("10.0")).build();
        PlanIngredientBatchUsage usage2 = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(activeBatch)
                .quantityUsed(new BigDecimal("5.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN_CANCEL"))
                .thenReturn(List.of(usage1, usage2));

        KitchenInventory inv = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient)
                .totalQuantity(new BigDecimal("5.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inv));

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN_CANCEL")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.cancelProductionPlan(
                "PLAN_CANCEL", "Cancel reason", mockPrincipal);

        assertNotNull(result);
        assertEquals("CANCELLED", plan.getStatus());

        assertEquals("ACTIVE", depletedBatch.getStatus());
        assertEquals(new BigDecimal("10.0"), depletedBatch.getRemainingQuantity());
        assertEquals("ACTIVE", activeBatch.getStatus());
        assertEquals(new BigDecimal("10.0"), activeBatch.getRemainingQuantity());

        assertTrue(plan.getNotes().contains("Cancel reason"));

        verify(planIngredientBatchUsageRepository).deleteAll(anyList());
    }

    /**
     * cancelProductionPlan - batch không phải DEPLETED → không đổi status.
     */
    @Test
    void cancelProductionPlan_BatchNotDepleted_StatusUnchanged() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLAN_ND").kitchen(mockKitchen).product(mockProduct)
                .status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLAN_ND")).thenReturn(Optional.of(plan));

        IngredientBatch batch = IngredientBatch.builder()
                .id("IB1").ingredient(mockIngredient)
                .status("ACTIVE").remainingQuantity(new BigDecimal("5.0")).build();

        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(batch)
                .quantityUsed(new BigDecimal("10.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN_ND"))
                .thenReturn(List.of(usage));

        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.empty());

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLAN_ND")).thenReturn(new ArrayList<>());

        centralKitchenService.cancelProductionPlan("PLAN_ND", null, mockPrincipal);

        assertEquals("ACTIVE", batch.getStatus());
        assertEquals(new BigDecimal("15.0"), batch.getRemainingQuantity());
    }

    // ===== FIX 8 (484-485): getProductBatchById - staff không có kitchen =====

    /**
     * getProductBatchById khi staff không có kitchen → bỏ qua check ownership.
     */
    @Test
    void getProductBatchById_StaffHasNoKitchen_ReturnsAnyBatch() {
        mockStaff.setKitchen(null);
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));

        BatchResponse result = centralKitchenService.getProductBatchById("BATCH001", mockPrincipal);

        assertNotNull(result);
        assertEquals("BATCH001", result.getId());
    }

    // ===== FIX 13: normalizeText - whitespace only =====

    /**
     * normalizeText với chuỗi chỉ có whitespace → trả về null.
     */
    @Test
    void getAllOrders_StatusIsWhitespaceOnly_TreatedAsNull() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("   ", null, 0, 20, mockPrincipal);

        assertNotNull(result);
        verify(orderRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getStores_NameIsWhitespaceOnly_TreatedAsNull() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Store> storePage = new PageImpl<>(List.of(mockStore));
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(storePage);

        Page<StoreResponse> result = centralKitchenService.getStores("  ", "  ", 0, 20, mockPrincipal);

        assertNotNull(result);
    }

    // ===== FIX 14: appendInternalNote - blank =====

    /**
     * appendInternalNote với oldNotes.isBlank() (rỗng hoặc chỉ whitespace).
     */
    @Test
    void updateOrderStatus_WithNotes_OldNotesIsBlank_CreatesNewNote() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order order = Order.builder()
                .id("ORD_BLANK").store(mockStore)
                .status(OrderStatus.IN_PROGRESS)
                .notes("   ")
                .build();
        when(orderRepository.findById("ORD_BLANK")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrder_Id("ORD_BLANK")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn("New note");

        centralKitchenService.updateOrderStatus("ORD_BLANK", request, mockPrincipal);

        assertTrue(order.getNotes().contains("New note"));
        assertFalse(order.getNotes().contains("\n"));
    }

    // ===== FIX 15: markOrderStatusTimestamp - PROCESSING status =====

    /**
     * markOrderStatusTimestamp với PROCESSING status.
     */
    @Test
    void updateOrderStatus_ToInProgress_FromApproved_SetsTimestampsForProcessingBranch() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order approvedOrder = Order.builder()
                .id("ORD_APPROVED").store(mockStore)
                .status(OrderStatus.APPROVED).build();
        when(orderRepository.findById("ORD_APPROVED")).thenReturn(Optional.of(approvedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(approvedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_APPROVED")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_APPROVED", request, mockPrincipal);

        assertNotNull(approvedOrder.getAssignedAt());
        assertNotNull(approvedOrder.getInProgressAt());
    }

    /**
     * Test legacy status PROCESSING → SHIPPING transition.
     */
    @Test
    void updateOrderStatus_FromProcessing_ToShipping_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order processingOrder = Order.builder()
                .id("ORD_PROC2").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.PROCESSING).build();
        when(orderRepository.findById("ORD_PROC2")).thenReturn(Optional.of(processingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(processingOrder);
        when(orderItemRepository.findByOrder_Id("ORD_PROC2")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.SHIPPING);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD_PROC2", request, mockPrincipal);

        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPING, processingOrder.getStatus());
    }

    // ===== FIX 16: toOrderResponse - null fields =====

    /**
     * toOrderResponse khi order.getStore() == null và kitchen == null.
     */
    @Test
    void getOrderById_OrderWithNullStore_ReturnsNullStoreFields() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order orderNoStore = Order.builder()
                .id("ORD_NOSTORE").store(null).kitchen(null)
                .status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD_NOSTORE")).thenReturn(Optional.of(orderNoStore));
        when(orderItemRepository.findByOrder_Id("ORD_NOSTORE")).thenReturn(new ArrayList<>());

        OrderResponse result = centralKitchenService.getOrderById("ORD_NOSTORE", mockPrincipal);

        assertNotNull(result);
        assertNull(result.getStoreId());
        assertNull(result.getStoreName());
        assertNull(result.getKitchenId());
        assertNull(result.getKitchenName());
    }

    // ===== FIX 17: toProductionPlanResponse - null fields =====

    /**
     * toProductionPlanResponse với product hoặc kitchen null.
     */
    @Test
    void getProductionPlans_PlanWithNullProductAndKitchen_ReturnsNullFields() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_NULL").product(null).kitchen(null).build();
        Page<ProductionPlan> planPage = new PageImpl<>(List.of(plan));
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(planPage);
        when(planIngredientRepository.findByPlan_Id("PLN_NULL")).thenReturn(new ArrayList<>());

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, mockPrincipal);

        assertNotNull(result);
        ProductionPlanResponse response = result.getContent().get(0);
        assertNull(response.getProductId());
        assertNull(response.getProductName());
        assertNull(response.getKitchenId());
        assertNull(response.getKitchenName());
    }

    /**
     * toProductionPlanResponse với pis == null (planIngredientRepository trả về null).
     */
    @Test
    void getProductionPlanById_PlanIngredientsReturnNull_HandlesGracefully() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN001").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN001")).thenReturn(Optional.of(plan));
        when(planIngredientRepository.findByPlan_Id("PLN001")).thenReturn(null);

        ProductionPlanResponse result = centralKitchenService.getProductionPlanById("PLN001", mockPrincipal);

        assertNotNull(result);
        assertNull(result.getIngredients());
    }

    // ===== FIX 18: toBatchResponse - batch fields null =====

    /**
     * toBatchResponse với product/kitchen null.
     */
    @Test
    void getProductBatchById_BatchWithNullProductAndKitchen_ReturnsNullFields() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        mockStaff.setKitchen(null);

        Batch batch = Batch.builder()
                .id("BATCH_NULL").plan(null).product(null).kitchen(null).build();
        when(batchRepository.findById("BATCH_NULL")).thenReturn(Optional.of(batch));

        BatchResponse result = centralKitchenService.getProductBatchById("BATCH_NULL", mockPrincipal);

        assertNotNull(result);
        assertNull(result.getPlanId());
        assertNull(result.getProductId());
        assertNull(result.getProductName());
        assertNull(result.getKitchenId());
        assertNull(result.getKitchenName());
        assertNull(result.getIngredientBatchUsages());
    }

    // ===== FIX 19: handleProductDeductionOnPacking - edge cases =====

    /**
     * handleProductDeductionOnPacking - batch dùng hết (DISTRIBUTED).
     */
    @Test
    void handleProductDeductionOnPacking_ExactlyDepletesBatch_SetsDistributed() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order order = Order.builder()
                .id("ORD_EXACT").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_EXACT")).thenReturn(Optional.of(order));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(50).build();
        when(orderItemRepository.findByOrder_Id("ORD_EXACT")).thenReturn(List.of(item));

        Batch batch = Batch.builder()
                .id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_EXACT", request, mockPrincipal);

        assertEquals(0, batch.getRemainingQuantity());
        assertEquals("DISTRIBUTED", batch.getStatus());
    }

    /**
     * handleProductDeductionOnPacking với nhiều sản phẩm khác nhau.
     */
    @Test
    void handleProductDeductionOnPacking_MultipleProducts_DeductsEachProduct() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order order = Order.builder()
                .id("ORD_MULTI").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_MULTI")).thenReturn(Optional.of(order));

        Product prod2 = Product.builder().id("PROD002").name("Bánh Bao").unit("cái").build();

        OrderItem item1 = OrderItem.builder().id(1).product(mockProduct).quantity(10).build();
        OrderItem item2 = OrderItem.builder().id(2).product(prod2).quantity(15).build();
        when(orderItemRepository.findByOrder_Id("ORD_MULTI")).thenReturn(List.of(item1, item2));

        Batch batch1 = Batch.builder()
                .id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(50).status("AVAILABLE").build();
        Batch batch2 = Batch.builder()
                .id("B2").product(prod2).kitchen(mockKitchen)
                .remainingQuantity(30).status("AVAILABLE").build();

        when(batchRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(batch1))
                .thenReturn(List.of(batch2));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        OrderResponse result = centralKitchenService.updateOrderStatus("ORD_MULTI", request, mockPrincipal);

        assertNotNull(result);
        verify(batchRepository, times(2)).save(any(Batch.class));
    }

    // ===== FIX 11: Specification methods via getOverview =====

    /**
     * Test requestedDateBetween với cả fromDate và toDate.
     */
    @Test
    void getOverview_WithBothDates_FiltersCorrectly() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(10L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), mockPrincipal);

        assertNotNull(result);
        verify(orderRepository, atLeast(6)).count(any(Specification.class));
    }

    // ===== Additional tests for complete coverage =====

    @Test
    void getProducts_NullSearchAndCategory_ReturnsAll() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Product> productPage = new PageImpl<>(List.of(mockProduct));
        when(productRepository.searchProducts(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().build());

        Page<ProductResponse> result = centralKitchenService.getProducts(null, null, 0, 20, mockPrincipal);

        assertNotNull(result);
        verify(productRepository).searchProducts(isNull(), isNull(), any(PageRequest.class));
    }

    /**
     * getProductInventory - nhiều batch cùng 1 product.
     */
    @Test
    void getProductInventory_MultipleBatchesSameProduct_AggregatesCorrectly() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch b1 = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").expiryDate(LocalDate.now().plusDays(5)).build();
        Batch b2 = Batch.builder().id("B2").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(20).status("AVAILABLE").expiryDate(LocalDate.now().plusDays(10)).build();
        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(b1, b2));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 0, 20, mockPrincipal);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        KitchenProductInventoryResponse response = result.getContent().get(0);
        assertEquals(30, response.getTotalRemainingQuantity());
        assertEquals(2, response.getBatches().size());
    }

    /**
     * getProductInventory - page size lớn hơn kết quả.
     */
    @Test
    void getProductInventory_PageSizeLargerThanResults_ReturnsAllInOnePage() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch b1 = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(b1));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 0, 100, mockPrincipal);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    /**
     * toBatchResponse với plan có usages.
     */
    @Test
    void getProductBatchById_WithPlanAndUsages_ReturnsUsages() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder().id("PLAN001").build();
        Batch batch = Batch.builder()
                .id("BATCH001").plan(plan).product(mockProduct).kitchen(mockKitchen).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));

        IngredientBatch ingredientBatch = IngredientBatch.builder()
                .id("IB001").ingredient(mockIngredient).batchNo("BN001")
                .unit("kg").expiryDate(LocalDate.now().plusMonths(1)).build();
        PlanIngredientBatchUsage usage = PlanIngredientBatchUsage.builder()
                .ingredientBatch(ingredientBatch).quantityUsed(new BigDecimal("5.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLAN001")).thenReturn(List.of(usage));

        BatchResponse result = centralKitchenService.getProductBatchById("BATCH001", mockPrincipal);

        assertNotNull(result);
        assertNotNull(result.getIngredientBatchUsages());
        assertEquals(1, result.getIngredientBatchUsages().size());
        assertEquals("IB001", result.getIngredientBatchUsages().get(0).getIngredientBatchId());
    }

    /**
     * updateBatch với tất cả các fields.
     */
    @Test
    void updateBatch_WithAllFields_UpdatesCorrectly() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Batch batch = Batch.builder()
                .id("BATCH001").kitchen(mockKitchen).product(mockProduct)
                .status("AVAILABLE").notes("Old notes").expiryDate(LocalDate.now()).build();
        when(batchRepository.findById("BATCH001")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        UpdateBatchRequest request = new UpdateBatchRequest();
        request.setExpiryDate(LocalDate.now().plusMonths(1));
        request.setStatus("DISTRIBUTED");
        request.setNotes("New notes");

        BatchResponse result = centralKitchenService.updateBatch("BATCH001", request, mockPrincipal);

        assertNotNull(result);
        assertEquals(LocalDate.now().plusMonths(1), batch.getExpiryDate());
        assertEquals("DISTRIBUTED", batch.getStatus());
        assertEquals("New notes", batch.getNotes());
    }
    // ==================== ADDITIONAL MISSING COVERAGE TESTS ====================

    // ===== 1. getAllOrders - storeId tồn tại (happy path branch) =====

    /**
     * getAllOrders với storeId hợp lệ - xác nhận nhánh existsById = true không throw.
     */
    @Test
    void getAllOrders_ValidStoreId_DoesNotThrow() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(storeRepository.existsById("ST001")).thenReturn(true); // branch: tồn tại → không throw

        Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> centralKitchenService.getAllOrders(null, "ST001", 0, 20, mockPrincipal));
    }

    // ===== 2. updateOrderStatus - explicit PACKED_WAITING_SHIPPER deduction call =====

    /**
     * updateOrderStatus từ PROCESSING → PACKED_WAITING_SHIPPER → gọi handleProductDeductionOnPacking.
     */
    @Test
    void updateOrderStatus_FromProcessing_ToPackedWaitingShipper_DeductsProduct() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order processingOrder = Order.builder()
                .id("ORD_PROC_PACK").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.PROCESSING).build();
        when(orderRepository.findById("ORD_PROC_PACK")).thenReturn(Optional.of(processingOrder));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(5).build();
        when(orderItemRepository.findByOrder_Id("ORD_PROC_PACK")).thenReturn(List.of(item));

        Batch batch = Batch.builder()
                .id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(20).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(processingOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_PROC_PACK", request, mockPrincipal);

        assertEquals(OrderStatus.PACKED_WAITING_SHIPPER, processingOrder.getStatus());
        assertEquals(15, batch.getRemainingQuantity());
        assertEquals("PART_DIST", batch.getStatus());
        verify(batchRepository, atLeastOnce()).save(any(Batch.class));
    }

    // ===== 3. createProductionPlan - không đủ ingredient (remainingToFulfill > 0) =====

    /**
     * createProductionPlan khi ingredient batch rỗng → remainingToFulfill > 0 → throw.
     */
    @Test
    void createProductionPlan_EmptyBatchList_ThrowsInsufficientIngredient() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("5.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        // Không có batch nào trong kho
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc("KIT001", "ING001"))
                .thenReturn(new ArrayList<>());

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));

        assertTrue(ex.getMessage().contains("Not enough ingredient"));
        assertTrue(ex.getMessage().contains("Bột mì"));
    }

    /**
     * createProductionPlan khi batch có nhưng không đủ số lượng → throw.
     */
    @Test
    void createProductionPlan_InsufficientBatchQuantity_ThrowsException() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(mockProduct));

        // recipe cần 5kg * 10 = 50kg, nhưng chỉ có 20kg
        Recipe recipe = Recipe.builder()
                .product(mockProduct).ingredient(mockIngredient)
                .quantity(new BigDecimal("5.0")).unit("kg").build();
        when(recipeRepository.findAllByProduct_Id("PROD001")).thenReturn(List.of(recipe));

        IngredientBatch batch = IngredientBatch.builder()
                .id("B001").ingredient(mockIngredient)
                .remainingQuantity(new BigDecimal("20.0")).build();
        when(ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc("KIT001", "ING001"))
                .thenReturn(List.of(batch));

        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .productId("PROD001").quantity(10)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                centralKitchenService.createProductionPlan(request, mockPrincipal));

        assertTrue(ex.getMessage().contains("Not enough ingredient"));
        assertTrue(ex.getMessage().contains("Short by: 30"));
    }

    // ===== 4. cancelProductionPlan - full IN_PRODUCTION flow với nhiều usages =====

    /**
     * cancelProductionPlan từ IN_PRODUCTION với nhiều batch:
     * - 1 batch DEPLETED → restore về ACTIVE
     * - 1 batch ACTIVE → giữ nguyên ACTIVE, chỉ cộng thêm qty
     */
    @Test
    void cancelProductionPlan_InProduction_MultipleUsages_RestoresAll() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_MULTI").kitchen(mockKitchen).product(mockProduct)
                .status("IN_PRODUCTION").build();
        when(productionPlanRepository.findById("PLN_MULTI")).thenReturn(Optional.of(plan));

        Ingredient ing2 = Ingredient.builder().id("ING002").name("Muối").build();

        IngredientBatch depleted = IngredientBatch.builder()
                .id("IB1").ingredient(mockIngredient)
                .status("DEPLETED").remainingQuantity(BigDecimal.ZERO).build();
        IngredientBatch active = IngredientBatch.builder()
                .id("IB2").ingredient(ing2)
                .status("ACTIVE").remainingQuantity(new BigDecimal("3.0")).build();

        PlanIngredientBatchUsage u1 = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(depleted).quantityUsed(new BigDecimal("8.0")).build();
        PlanIngredientBatchUsage u2 = PlanIngredientBatchUsage.builder()
                .plan(plan).ingredientBatch(active).quantityUsed(new BigDecimal("2.0")).build();
        when(planIngredientBatchUsageRepository.findByPlan_Id("PLN_MULTI"))
                .thenReturn(List.of(u1, u2));

        KitchenInventory inv1 = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(mockIngredient).totalQuantity(BigDecimal.ZERO).build();
        KitchenInventory inv2 = KitchenInventory.builder()
                .kitchen(mockKitchen).ingredient(ing2).totalQuantity(new BigDecimal("3.0")).build();
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING001"))
                .thenReturn(Optional.of(inv1));
        when(kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id("KIT001", "ING002"))
                .thenReturn(Optional.of(inv2));

        when(productionPlanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planIngredientRepository.findByPlan_Id("PLN_MULTI")).thenReturn(new ArrayList<>());

        ProductionPlanResponse result = centralKitchenService.cancelProductionPlan("PLN_MULTI", null, mockPrincipal);

        assertNotNull(result);
        assertEquals("CANCELLED", plan.getStatus());

        // batch DEPLETED → ACTIVE sau khi restore 8kg
        assertEquals("ACTIVE", depleted.getStatus());
        assertEquals(new BigDecimal("8.0"), depleted.getRemainingQuantity());

        // batch ACTIVE → vẫn ACTIVE, qty cộng thêm 2
        assertEquals("ACTIVE", active.getStatus());
        assertEquals(new BigDecimal("5.0"), active.getRemainingQuantity());

        // Inventory được cộng lại
        assertEquals(new BigDecimal("8.0"), inv1.getTotalQuantity());
        assertEquals(new BigDecimal("5.0"), inv2.getTotalQuantity());

        verify(ingredientBatchRepository, times(2)).save(any(IngredientBatch.class));
        verify(kitchenInventoryRepository, times(2)).save(any(KitchenInventory.class));
        verify(planIngredientBatchUsageRepository).deleteAll(anyList());
    }

    // ===== 5. getProductInventory - pagination grouping logic =====

    /**
     * getProductInventory - nhiều sản phẩm, page=0, size=2 → trả đúng 2 phần tử đầu theo alphabet.
     */
    @Test
    void getProductInventory_MultipleProducts_SortedAlphabetically() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Product prodA = Product.builder().id("P_A").name("An Bánh").unit("cái").build();
        Product prodB = Product.builder().id("P_B").name("Bánh Bao").unit("cái").build();
        Product prodC = Product.builder().id("P_C").name("Cơm Tấm").unit("phần").build();

        Batch bA = Batch.builder().id("BA").product(prodA).kitchen(mockKitchen)
                .remainingQuantity(5).status("AVAILABLE").build();
        Batch bB = Batch.builder().id("BB").product(prodB).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        Batch bC = Batch.builder().id("BC").product(prodC).kitchen(mockKitchen)
                .remainingQuantity(15).status("AVAILABLE").build();

        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(bC, bA, bB)); // trả về lộn xộn

        // page=0, size=2 → chỉ lấy 2 sản phẩm đầu tiên theo alphabet
        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 0, 2, mockPrincipal);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        // Kết quả phải được sắp xếp theo tên alphabet
        assertEquals("An Bánh", result.getContent().get(0).getProductName());
        assertEquals("Bánh Bao", result.getContent().get(1).getProductName());
    }

    /**
     * getProductInventory - page=1, size=2 → trang thứ 2 chỉ còn 1 phần tử.
     */
    @Test
    void getProductInventory_SecondPage_ReturnsRemainingElement() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Product prodA = Product.builder().id("P_A").name("An Bánh").unit("cái").build();
        Product prodB = Product.builder().id("P_B").name("Bánh Bao").unit("cái").build();
        Product prodC = Product.builder().id("P_C").name("Cơm Tấm").unit("phần").build();

        Batch bA = Batch.builder().id("BA").product(prodA).kitchen(mockKitchen)
                .remainingQuantity(5).status("AVAILABLE").build();
        Batch bB = Batch.builder().id("BB").product(prodB).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        Batch bC = Batch.builder().id("BC").product(prodC).kitchen(mockKitchen)
                .remainingQuantity(15).status("AVAILABLE").build();

        when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(bA, bB, bC));

        Page<KitchenProductInventoryResponse> result = centralKitchenService.getProductInventory(
                null, null, 1, 2, mockPrincipal);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Cơm Tấm", result.getContent().get(0).getProductName());
    }

    // ===== 6. getStores - filter by status =====

    /**
     * getStores với status filter → Specification được thêm đúng.
     */
    @Test
    void getStores_WithStatusFilter_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Store activeStore = Store.builder()
                .id("ST_ACT").name("Active Store").status("ACTIVE").build();
        Page<Store> storePage = new PageImpl<>(List.of(activeStore));
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(storePage);

        Page<StoreResponse> result = centralKitchenService.getStores(null, "ACTIVE", 0, 20, mockPrincipal);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(storeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    /**
     * getStores với name filter → Specification được thêm đúng.
     */
    @Test
    void getStores_WithNameFilter_Success() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Page<Store> storePage = new PageImpl<>(List.of(mockStore));
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(storePage);

        Page<StoreResponse> result = centralKitchenService.getStores("Franchise", null, 0, 20, mockPrincipal);

        assertNotNull(result);
        verify(storeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ===== 7. requestedDateBetween - chỉ fromDate hoặc chỉ toDate =====

    /**
     * getOverview với chỉ fromDate → requestedDateBetween chỉ có greaterThanOrEqualTo.
     */
    @Test
    void getOverview_OnlyFromDate_RequestedDateBetweenBranch() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(3L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                LocalDate.of(2026, 1, 1), null, mockPrincipal);

        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
        verify(orderRepository, atLeast(6)).count(any(Specification.class));
    }

    /**
     * getOverview với chỉ toDate → requestedDateBetween chỉ có lessThanOrEqualTo.
     */
    @Test
    void getOverview_OnlyToDate_RequestedDateBetweenBranch() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));
        when(orderRepository.count(any(Specification.class))).thenReturn(5L);

        CentralKitchenOverviewResponse result = centralKitchenService.getOverview(
                null, LocalDate.of(2026, 12, 31), mockPrincipal);

        assertNotNull(result);
        assertEquals("KIT001", result.getKitchenId());
    }

    // ===== 8. markOrderStatusTimestamp - PACKED_WAITING_SHIPPER explicit =====

    /**
     * markOrderStatusTimestamp PACKED_WAITING_SHIPPER - set 3 timestamps đầu.
     */
    @Test
    void updateOrderStatus_ToPackedWaitingShipper_SetsPackedTimestamp() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order inProgressOrder = Order.builder()
                .id("ORD_PKG_TS").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_PKG_TS")).thenReturn(Optional.of(inProgressOrder));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(1).build();
        when(orderItemRepository.findByOrder_Id("ORD_PKG_TS")).thenReturn(List.of(item));

        Batch batch = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_PKG_TS", request, mockPrincipal);

        assertNotNull(inProgressOrder.getAssignedAt());
        assertNotNull(inProgressOrder.getInProgressAt());
        assertNotNull(inProgressOrder.getPackedWaitingShipperAt());
    }

    /**
     * markOrderStatusTimestamp PACKED_WAITING_SHIPPER - các timestamp đã có → không ghi đè.
     */
    @Test
    void updateOrderStatus_ToPackedWaitingShipper_ExistingTimestamps_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingAssigned = LocalDateTime.now().minusHours(4);
        LocalDateTime existingInProgress = LocalDateTime.now().minusHours(3);
        LocalDateTime existingPacked = LocalDateTime.now().minusHours(2);

        Order inProgressOrder = Order.builder()
                .id("ORD_PKG_EXIST").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS)
                .assignedAt(existingAssigned)
                .inProgressAt(existingInProgress)
                .packedWaitingShipperAt(existingPacked)
                .build();
        when(orderRepository.findById("ORD_PKG_EXIST")).thenReturn(Optional.of(inProgressOrder));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(1).build();
        when(orderItemRepository.findByOrder_Id("ORD_PKG_EXIST")).thenReturn(List.of(item));

        Batch batch = Batch.builder().id("B1").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(10).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_PKG_EXIST", request, mockPrincipal);

        assertEquals(existingAssigned, inProgressOrder.getAssignedAt());
        assertEquals(existingInProgress, inProgressOrder.getInProgressAt());
        assertEquals(existingPacked, inProgressOrder.getPackedWaitingShipperAt());
    }

    /**
     * markOrderStatusTimestamp SHIPPING - các timestamp đã có → không ghi đè.
     */
    @Test
    void updateOrderStatus_ToShipping_ExistingTimestamps_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existing = LocalDateTime.now().minusHours(3);

        Order packedOrder = Order.builder()
                .id("ORD_SHIP_EX").store(mockStore)
                .status(OrderStatus.PACKED_WAITING_SHIPPER)
                .assignedAt(existing).inProgressAt(existing)
                .packedWaitingShipperAt(existing).shippingAt(existing).build();
        when(orderRepository.findById("ORD_SHIP_EX")).thenReturn(Optional.of(packedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(packedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_SHIP_EX")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.SHIPPING);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_SHIP_EX", request, mockPrincipal);

        assertEquals(existing, packedOrder.getAssignedAt());
        assertEquals(existing, packedOrder.getInProgressAt());
        assertEquals(existing, packedOrder.getPackedWaitingShipperAt());
        assertEquals(existing, packedOrder.getShippingAt());
    }

    /**
     * markOrderStatusTimestamp CANCELLED - cancelledAt đã có → không ghi đè.
     */
    @Test
    void updateOrderStatus_ToCancelled_ExistingCancelledAt_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingCancelledAt = LocalDateTime.now().minusHours(1);
        Order assignedOrder = Order.builder()
                .id("ORD_CANCEL_EX").store(mockStore)
                .status(OrderStatus.ASSIGNED)
                .cancelledAt(existingCancelledAt).build();
        when(orderRepository.findById("ORD_CANCEL_EX")).thenReturn(Optional.of(assignedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(assignedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_CANCEL_EX")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_CANCEL_EX", request, mockPrincipal);

        assertEquals(existingCancelledAt, assignedOrder.getCancelledAt());
    }

    // ===== 9. markOrderStatusTimestamp - IN_PROGRESS, PROCESSING timestamps đã có =====

    /**
     * markOrderStatusTimestamp IN_PROGRESS - assignedAt và inProgressAt đã có → không ghi đè.
     */
    @Test
    void updateOrderStatus_ToInProgress_ExistingTimestamps_NotOverwritten() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDateTime existingAssigned = LocalDateTime.now().minusHours(2);
        LocalDateTime existingInProgress = LocalDateTime.now().minusHours(1);

        Order assignedOrder = Order.builder()
                .id("ORD_IP_EX").store(mockStore)
                .status(OrderStatus.ASSIGNED)
                .assignedAt(existingAssigned)
                .inProgressAt(existingInProgress)
                .build();
        when(orderRepository.findById("ORD_IP_EX")).thenReturn(Optional.of(assignedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(assignedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_IP_EX")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_IP_EX", request, mockPrincipal);

        assertEquals(existingAssigned, assignedOrder.getAssignedAt());
        assertEquals(existingInProgress, assignedOrder.getInProgressAt());
    }

    /**
     * markOrderStatusTimestamp PROCESSING (legacy) - set assignedAt và inProgressAt.
     */
    @Test
    void updateOrderStatus_ToProcessing_FromApproved_SetsTimestamps() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        // APPROVED → SHIPPING (legacy, không đi qua IN_PROGRESS/PROCESSING branch trong switch)
        // Để test PROCESSING case trong switch: dùng APPROVED → IN_PROGRESS (trigger IN_PROGRESS/PROCESSING case)
        Order approvedOrder = Order.builder()
                .id("ORD_PROC_TS").store(mockStore)
                .status(OrderStatus.APPROVED).build();
        when(orderRepository.findById("ORD_PROC_TS")).thenReturn(Optional.of(approvedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(approvedOrder);
        when(orderItemRepository.findByOrder_Id("ORD_PROC_TS")).thenReturn(new ArrayList<>());

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.IN_PROGRESS);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_PROC_TS", request, mockPrincipal);

        // Cả IN_PROGRESS và PROCESSING dùng chung case trong switch
        assertNotNull(approvedOrder.getAssignedAt());
        assertNotNull(approvedOrder.getInProgressAt());
    }

    // ===== 10. toProductionPlanResponse - available đủ → isSufficient = true =====

    /**
     * toProductionPlanResponse - available >= required → isSufficient = true.
     */
    @Test
    void getProductionPlanById_WithIngredients_Available_Sufficient_True() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        ProductionPlan plan = ProductionPlan.builder()
                .id("PLN_SUFF").kitchen(mockKitchen).product(mockProduct).build();
        when(productionPlanRepository.findById("PLN_SUFF")).thenReturn(Optional.of(plan));

        PlanIngredient pi = PlanIngredient.builder()
                .id(1).plan(plan).ingredient(mockIngredient)
                .quantity(new BigDecimal("10.0")).unit("kg").build();
        when(planIngredientRepository.findByPlan_Id("PLN_SUFF")).thenReturn(List.of(pi));

        // available >= required → sufficient = true
        when(ingredientBatchRepository.sumRemainingByKitchenAndIngredient("KIT001", "ING001"))
                .thenReturn(new BigDecimal("15.0"));

        ProductionPlanResponse result = centralKitchenService.getProductionPlanById("PLN_SUFF", mockPrincipal);

        assertNotNull(result);
        assertNotNull(result.getIngredients());
        assertEquals(1, result.getIngredients().size());
        assertTrue(result.getIngredients().get(0).isSufficient());
        assertEquals(new BigDecimal("15.0"), result.getIngredients().get(0).getAvailableQuantity());
    }

    // ===== 11. handleProductDeductionOnPacking - PARTIALLY_DISTRIBUTED explicit =====

    /**
     * handleProductDeductionOnPacking - 1 batch lớn hơn nhu cầu → PARTIALLY_DISTRIBUTED.
     */
    @Test
    void handleProductDeductionOnPacking_SingleLargeBatch_BecomesPartiallyDistributed() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order order = Order.builder()
                .id("ORD_PARTIAL").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_PARTIAL")).thenReturn(Optional.of(order));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(20).build();
        when(orderItemRepository.findByOrder_Id("ORD_PARTIAL")).thenReturn(List.of(item));

        // Batch có 100, chỉ cần 20 → còn lại 80, PARTIALLY_DISTRIBUTED
        Batch batch = Batch.builder()
                .id("B_LARGE").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(100).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_PARTIAL", request, mockPrincipal);

        assertEquals("PART_DIST", batch.getStatus());
        assertEquals(80, batch.getRemainingQuantity());
    }

    /**
     * handleProductDeductionOnPacking - batch 0 remaining sau khi trừ → DISTRIBUTED.
     */
    @Test
    void handleProductDeductionOnPacking_BatchFullyConsumed_BecomesDistributed() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        Order order = Order.builder()
                .id("ORD_FULL").store(mockStore).kitchen(mockKitchen)
                .status(OrderStatus.IN_PROGRESS).build();
        when(orderRepository.findById("ORD_FULL")).thenReturn(Optional.of(order));

        OrderItem item = OrderItem.builder().id(1).product(mockProduct).quantity(30).build();
        when(orderItemRepository.findByOrder_Id("ORD_FULL")).thenReturn(List.of(item));

        // Batch có đúng 30 → dùng hết → DISTRIBUTED
        Batch batch = Batch.builder()
                .id("B_EXACT").product(mockProduct).kitchen(mockKitchen)
                .remainingQuantity(30).status("AVAILABLE").build();
        when(batchRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest request = mock(UpdateOrderStatusRequest.class);
        when(request.getStatus()).thenReturn(OrderStatus.PACKED_WAITING_SHIPPER);
        when(request.getNotes()).thenReturn(null);

        centralKitchenService.updateOrderStatus("ORD_FULL", request, mockPrincipal);

        assertEquals("DISTRIBUTED", batch.getStatus());
        assertEquals(0, batch.getRemainingQuantity());
    }

    // ===== 12. updateBatch - fields null → không update =====

    /**
     * updateBatch với request rỗng (tất cả null) → không thay đổi batch fields.
     */
    @Test
    void updateBatch_AllFieldsNull_DoesNotChangeExistingValues() {
        when(mockPrincipal.getName()).thenReturn("staff");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(mockStaff));

        LocalDate originalExpiry = LocalDate.now().plusMonths(2);
        Batch batch = Batch.builder()
                .id("BATCH_NO_CHANGE").kitchen(mockKitchen).product(mockProduct)
                .expiryDate(originalExpiry).status("AVAILABLE").notes("Original note").build();
        when(batchRepository.findById("BATCH_NO_CHANGE")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planIngredientBatchUsageRepository.findByPlan_Id(any())).thenReturn(new ArrayList<>());

        // request với tất cả fields null
        UpdateBatchRequest request = new UpdateBatchRequest();
        // request.getExpiryDate() = null, request.getStatus() = null, request.getNotes() = null

        BatchResponse result = centralKitchenService.updateBatch("BATCH_NO_CHANGE", request, mockPrincipal);

        assertNotNull(result);
        // Giá trị gốc không bị thay đổi
        assertEquals(originalExpiry, batch.getExpiryDate());
        assertEquals("AVAILABLE", batch.getStatus());
        assertEquals("Original note", batch.getNotes());
    }
}