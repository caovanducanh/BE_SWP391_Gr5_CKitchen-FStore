package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.request.store.OrderItemRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.OrderTimelineResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.dto.response.StoreOverviewResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.entity.*;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.*;
import com.example.demologin.mapper.ProductMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FranchiseStoreServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private StoreInventoryRepository storeInventoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private KitchenRepository kitchenRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderPriorityConfigRepository orderPriorityConfigRepository;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private FranchiseStoreServiceImpl franchiseStoreService;

    private Store store;
    private Kitchen kitchen;
    private Product product;
    private Principal principal;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();
        store = Store.builder().id("ST001").name("Store 1").build();
        product = Product.builder().id("PROD001").name("Product 1").unit("piece").cost(BigDecimal.valueOf(10)).build();
        principal = mock(Principal.class);
    }

    private void mockCurrentStore(String username, Store store) {
        when(principal.getName()).thenReturn(username);
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setStore(store);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        // Arrange
        mockCurrentStore("testuser", store);
        
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(1));
        when(request.getNotes()).thenReturn("Notes");
        when(request.getItems()).thenReturn(List.of(itemRequest));


        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(1));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderPriorityConfigRepository.findAll()).thenReturn(List.of(
                OrderPriorityConfig.builder().priorityCode("HIGH").minDays(0).maxDays(0).build(),
                OrderPriorityConfig.builder().priorityCode("NORMAL").minDays(1).maxDays(2).build(),
                OrderPriorityConfig.builder().priorityCode("LOW").minDays(3).maxDays(null).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.count()).thenReturn(100L);

        // Act
        OrderResponse response = franchiseStoreService.createOrder(request, principal);

        // Assert
        assertNotNull(response);
        assertEquals("ST001", response.getStoreId());
        assertNull(response.getKitchenId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals("NORMAL", response.getPriority()); // requestedDate is tomorrow (1 day) -> NORMAL
        assertEquals(1, response.getItems().size());
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    void createOrder_shouldThrowExceptionWhenNotStaff() {
        when(principal.getName()).thenReturn("manager");
        User user = new User();
        user.setUsername("manager");
        user.setRole(Role.builder().name("manager").build());

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        CreateOrderRequest request = mock(CreateOrderRequest.class);

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.createOrder(request, principal));
    }

    @Test
    void getOrders_shouldReturnPagedOrders() {
        // Arrange
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        // Act
        Page<OrderResponse> result = franchiseStoreService.getOrders(null, principal, 0, 10);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("ORD001", result.getContent().get(0).getId());
    }

    @Test
    void getOrderById_shouldReturnOrder() {
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        OrderResponse response = franchiseStoreService.getOrderById("ORD001");

        assertEquals("ORD001", response.getId());
    }

    @Test
    void confirmReceipt_shouldUpdateDeliveryAndOrderStatus() {
        // Arrange
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();
        
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);

        // Assert
        assertEquals("DELIVERED", response.getStatus());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void getStoreInventory_shouldReturnInventory() {
        // Arrange
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));
        
        when(storeInventoryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory(null, null, principal, 0, 10);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLowStock());
        verify(storeInventoryRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }
    @Test
    void getOrders_shouldThrowExceptionWhenNotStaff() {
        when(principal.getName()).thenReturn("testuser");
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOrders(null, principal, 0, 10));
    }

    @Test
    void getOrders_shouldReturnPagedOrdersWithStatus() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        
        when(orderRepository.findByStore_IdAndStatus(eq("ST001"), eq(OrderStatus.PENDING), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = franchiseStoreService.getOrders("pending", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void confirmReceipt_shouldUpdateDeliveryWithNotes() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();
        
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);
        when(request.getNotes()).thenReturn("Some notes");

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);
        assertEquals("DELIVERED", response.getStatus());
        assertEquals("Some notes", response.getNotes());
    }

    @Test
    void getStoreInventory_shouldThrowExceptionWhenNotStaff() {
        when(principal.getName()).thenReturn("testuser");
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getStoreInventory(null, null, principal, 0, 10));
    }

    @Test
    void getStoreInventory_shouldFilterByProductIdAndName() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder().id(1).store(store).product(product).quantity(5).minStock(10).build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));
        
        when(productRepository.existsById("PROD001")).thenReturn(true);
        when(storeInventoryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory("PROD001", "name", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldThrowExceptionWhenProductNotFound() {
        mockCurrentStore("testuser", store);
        when(productRepository.existsById("WRONG")).thenReturn(false);
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getStoreInventory("WRONG", null, principal, 0, 10));
    }

    @Test
    void getMyStore_shouldReturnStoreResponse() {
        mockCurrentStore("testuser", store);
        StoreResponse response = franchiseStoreService.getMyStore(principal);
        assertEquals("ST001", response.getId());
    }

    @Test
    void getMyStore_shouldThrowExceptionWhenNotAssociated() {
        when(principal.getName()).thenReturn("testuser");
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getMyStore(principal));
    }

    @Test
    void createOrder_shouldCalculatePriorityWithPastDateAsZeroWaitMsBeforeAsync() {
        mockCurrentStore("testuser", store);
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getRequestedDate()).thenReturn(LocalDate.now().minusDays(5));
        when(request.getItems()).thenReturn(List.of(itemRequest));

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderPriorityConfigRepository.findAll()).thenReturn(List.of(
                OrderPriorityConfig.builder().priorityCode("HIGH").minDays(0).maxDays(0).build(),
                OrderPriorityConfig.builder().priorityCode("EXTREME").minDays(0).maxDays(null).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = franchiseStoreService.createOrder(request, principal);
        assertEquals("HIGH", response.getPriority());
    }
    @Test
    void createOrder_shouldThrowExceptionWhenProductNotFound_PreCheck() {
        mockCurrentStore("testuser", store);
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getItems()).thenReturn(List.of(itemRequest));
        when(request.getRequestedDate()).thenReturn(LocalDate.now());
        
        when(productRepository.findById("PROD001")).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> franchiseStoreService.createOrder(request, principal));
    }

    @Test
    void createOrder_shouldThrowExceptionWhenProductNotFound_InBuildOrderItem() {
        mockCurrentStore("testuser", store);
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);
        
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getItems()).thenReturn(List.of(itemRequest));
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(2));
        
        // Return present first time, empty second time to hit the buildOrderItem catch
        when(productRepository.findById("PROD001"))
            .thenReturn(Optional.of(product))
            .thenReturn(Optional.empty());
            
        assertThrows(NotFoundException.class, () -> franchiseStoreService.createOrder(request, principal));
    }

    @Test
    void getOrders_shouldReturnAllOrdersWhenStatusIsBlank() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = franchiseStoreService.getOrders("   ", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById("ORD001")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getOrderById("ORD001"));
    }

    @Test
    void getOrderTimeline_shouldReturnTimelineWhenOrderBelongsToCurrentStore() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.IN_PROGRESS)
                .assignedAt(LocalDateTime.now().minusHours(2))
                .inProgressAt(LocalDateTime.now().minusHours(1))
                .build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        OrderTimelineResponse response = franchiseStoreService.getOrderTimeline("ORD001", principal);

        assertEquals("ORD001", response.getOrderId());
        assertEquals(OrderStatus.IN_PROGRESS, response.getCurrentStatus());
        assertNotNull(response.getAssignedAt());
        assertNotNull(response.getInProgressAt());
    }

    @Test
    void getOrderTimeline_shouldThrowWhenOrderNotBelongToCurrentStore() {
        mockCurrentStore("testuser", store);
        Store anotherStore = Store.builder().id("ST999").name("Another").build();
        Order order = Order.builder().id("ORD001").store(anotherStore).status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(NotFoundException.class, () -> franchiseStoreService.getOrderTimeline("ORD001", principal));
    }

    @Test
    void getOrderTimeline_shouldThrowWhenOrderNotFound() {
        mockCurrentStore("testuser", store);
        when(orderRepository.findById("ORD404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> franchiseStoreService.getOrderTimeline("ORD404", principal));
    }

    @Test
    void confirmReceipt_shouldThrowExceptionWhenDeliveryNotFound() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> franchiseStoreService.confirmReceipt("DEL001", request));
    }

    @Test
    void getCurrentStore_shouldThrowExceptionWhenUserHasNoStore() {
        when(principal.getName()).thenReturn("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        User user = new User();
        user.setUsername("testuser");
        user.setRole(role);
        user.setStore(null); // Staff but no store
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getMyStore(principal));
    }

    @Test
    void getCurrentStore_shouldThrowExceptionWhenUserNotFound() {
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getMyStore(principal));
    }

    @Test
    void getStoreInventory_shouldExecuteSpecificationLambdaForCoverage() {
        mockCurrentStore("testuser", store);
        ArgumentCaptor<Specification<StoreInventory>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(storeInventoryRepository.findAll(specCaptor.capture(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(productRepository.existsById("PROD001")).thenReturn(true);
        franchiseStoreService.getStoreInventory("PROD001", "product", principal, 0, 10);

        Specification<StoreInventory> spec = specCaptor.getValue();
        Root<StoreInventory> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        
        when(root.get(anyString())).thenReturn(path);
        when(path.get(anyString())).thenReturn(path);
        jakarta.persistence.criteria.Predicate mockPredicate = mock(jakarta.persistence.criteria.Predicate.class);
        
        when(cb.equal(any(), any())).thenReturn(mockPredicate);
        when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class));
        when(cb.like(any(), anyString())).thenReturn(mockPredicate);
        when(cb.and(any(), any())).thenReturn(mockPredicate);

        spec.toPredicate(root, query, cb);
        
        ArgumentCaptor<jakarta.persistence.criteria.Expression> exprCaptor = ArgumentCaptor.forClass(jakarta.persistence.criteria.Expression.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(cb, atLeastOnce()).equal(exprCaptor.capture(), valueCaptor.capture());
        
        verify(cb, atLeastOnce()).like(any(jakarta.persistence.criteria.Expression.class), anyString());
    }

    @Test
    void getAvailableProducts_shouldReturnPagedProducts() {
        // Arrange
        Product product2 = Product.builder().id("PROD002").name("Product 2").build();
        Page<Product> productPage = new PageImpl<>(List.of(product, product2));
        
        when(productRepository.searchProducts(anyString(), any(), any(PageRequest.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        // Act
        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts("Product", "BAKERY", 0, 10);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(productRepository).searchProducts(eq("Product"), any(), any(PageRequest.class));
    }

    @Test
    void getDeliveries_shouldReturnPagedDeliveriesWithStatusFilter() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();
        Page<Delivery> deliveryPage = new PageImpl<>(List.of(delivery));

        when(deliveryRepository.findByOrder_Store_IdAndStatus(eq("ST001"), eq("SHIPPING"), any(PageRequest.class)))
                .thenReturn(deliveryPage);

        Page<DeliveryResponse> result = franchiseStoreService.getDeliveries("shipping", principal, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("DEL001", result.getContent().get(0).getId());
        verify(deliveryRepository).findByOrder_Store_IdAndStatus(eq("ST001"), eq("SHIPPING"), any(PageRequest.class));
    }

    @Test
    void getOverview_shouldReturnStoreOverviewMetrics() {
        mockCurrentStore("testuser", store);

        when(orderRepository.countByStore_Id("ST001")).thenReturn(12L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.PENDING)).thenReturn(2L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.IN_PROGRESS)).thenReturn(3L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.SHIPPING)).thenReturn(2L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.DELIVERED)).thenReturn(4L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.CANCELLED)).thenReturn(1L);
        when(storeInventoryRepository.countLowStockItemsByStoreId("ST001")).thenReturn(5L);
        when(deliveryRepository.countByOrder_Store_IdAndStatusIn(eq("ST001"), anyCollection())).thenReturn(2L);

        StoreOverviewResponse overview = franchiseStoreService.getOverview(principal);

        assertEquals("ST001", overview.getStoreId());
        assertEquals(12L, overview.getTotalOrders());
        assertEquals(5L, overview.getLowStockItems());
        assertEquals(2L, overview.getActiveDeliveries());

        verify(deliveryRepository).countByOrder_Store_IdAndStatusIn("ST001", Arrays.asList("ASSIGNED", "SHIPPING"));
    }

    @Test
    void getOrderStatuses_shouldReturnAllEnumStatuses() {
        List<String> statuses = franchiseStoreService.getOrderStatuses();
        assertEquals(OrderStatus.values().length, statuses.size());
        assertTrue(statuses.contains("PENDING"));
        assertTrue(statuses.contains("DELIVERED"));
    }
}
