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
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.ProductMapper;
import com.example.demologin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private User user;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();
        store = Store.builder().id("ST001").name("Store 1").address("123 Street").phone("0123456789").build();
        product = Product.builder()
                .id("PROD001")
                .name("Product 1")
                .unit("piece")
                .cost(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(20))
                .build();
        principal = mock(Principal.class);

        user = new User();
        user.setUsername("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        user.setRole(role);
        user.setStore(store);
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
        mockCurrentStore("testuser", store);

        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(1));
        when(request.getNotes()).thenReturn("Notes");
        when(request.getItems()).thenReturn(List.of(itemRequest));

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderPriorityConfigRepository.findAll()).thenReturn(List.of(
                OrderPriorityConfig.builder().priorityCode("HIGH").minDays(0).maxDays(0).build(),
                OrderPriorityConfig.builder().priorityCode("NORMAL").minDays(1).maxDays(2).build(),
                OrderPriorityConfig.builder().priorityCode("LOW").minDays(3).maxDays(null).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.count()).thenReturn(100L);

        OrderResponse response = franchiseStoreService.createOrder(request, principal);

        assertNotNull(response);
        assertEquals("ST001", response.getStoreId());
        assertNull(response.getKitchenId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals("NORMAL", response.getPriority());
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
    void createOrder_shouldCalculatePriorityWithPastDate() {
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
    void createOrder_shouldThrowExceptionWhenProductNotFound() {
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
    void createOrder_shouldUseDefaultPriorityWhenNoConfigMatches() {
        mockCurrentStore("testuser", store);
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(10));
        when(request.getItems()).thenReturn(List.of(itemRequest));

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderPriorityConfigRepository.findAll()).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.count()).thenReturn(100L);

        OrderResponse response = franchiseStoreService.createOrder(request, principal);
        assertEquals("NORMAL", response.getPriority());
    }

    @Test
    void createOrder_shouldHandleNotesAsNull() {
        mockCurrentStore("testuser", store);
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(1));
        when(request.getNotes()).thenReturn(null);
        when(request.getItems()).thenReturn(List.of(itemRequest));

        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderPriorityConfigRepository.findAll()).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.count()).thenReturn(100L);

        OrderResponse response = franchiseStoreService.createOrder(request, principal);
        assertNull(response.getNotes());
    }

    @Test
    void createOrder_shouldThrowWhenPrincipalIsNull() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.createOrder(request, null));
    }

    @Test
    void getOrders_shouldReturnPagedOrders() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = franchiseStoreService.getOrders(null, principal, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("ORD001", result.getContent().get(0).getId());
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
    void getOrders_shouldThrowWithInvalidStatus() {
        mockCurrentStore("testuser", store);
        assertThrows(BadRequestException.class, () -> franchiseStoreService.getOrders("INVALID_STATUS", principal, 0, 10));
    }

    @Test
    void getOrders_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User adminUser = new User();
        adminUser.setUsername("testuser");
        Role role = Role.builder().name("ADMIN").build();
        adminUser.setRole(role);
        adminUser.setStore(store);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOrders(null, principal, 0, 10));
    }

    @Test
    void getOrderById_shouldReturnOrder() {
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = franchiseStoreService.getOrderById("ORD001");
        assertEquals("ORD001", response.getId());
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById("ORD001")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getOrderById("ORD001"));
    }

    @Test
    void getOrderById_shouldHandleOrderWithNullKitchen() {
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .kitchen(null)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = franchiseStoreService.getOrderById("ORD001");
        assertNull(response.getKitchenId());
        assertNull(response.getKitchenName());
    }

    @Test
    void confirmReceipt_shouldUpdateDeliveryAndOrderStatus() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);

        assertEquals("DELIVERED", response.getStatus());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository).save(order);
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
    void confirmReceipt_shouldThrowExceptionWhenDeliveryNotFound() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> franchiseStoreService.confirmReceipt("DEL001", request));
    }

    @Test
    void confirmReceipt_shouldWorkWithWaitingConfirmStatus() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("WAITING_CONFIRM").build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);
        when(request.getNotes()).thenReturn("Test notes");

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);

        assertEquals("DELIVERED", response.getStatus());
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    void confirmReceipt_shouldThrowExceptionWhenInvalidStatus() {
        Order order = Order.builder().id("ORD001").build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("PENDING").build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> franchiseStoreService.confirmReceipt("DEL001", request));
    }

    @Test
    void confirmReceipt_shouldSetDeliveredAtIfNull() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("SHIPPING")
                .deliveredAt(null)
                .build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);
        assertNotNull(response.getDeliveredAt());
    }

    @Test
    void confirmReceipt_shouldHandleNullNotes() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);
        when(request.getNotes()).thenReturn(null);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);
        assertEquals("DELIVERED", response.getStatus());
        assertNull(response.getNotes());
    }

    @Test
    void confirmReceipt_shouldHandleDeliveryWithNullStatus() {
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status(null).build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> franchiseStoreService.confirmReceipt("DEL001", request));
    }

    @Test
    void confirmReceipt_shouldHandleExistingDeliveredAt() {
        LocalDateTime existingTime = LocalDateTime.now().minusDays(1);
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).deliveredAt(existingTime).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("SHIPPING")
                .deliveredAt(existingTime)
                .build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);
        assertEquals(existingTime, response.getDeliveredAt());
    }

    @Test
    void confirmReceiptByOrderId_shouldWorkSuccessfully() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal);
        assertEquals("DELIVERED", response.getStatus());
    }

    @Test
    void confirmReceiptByOrderId_shouldThrowWhenOrderNotBelongToStore() {
        mockCurrentStore("testuser", store);
        Store anotherStore = Store.builder().id("ST999").build();
        Order order = Order.builder().id("ORD001").store(anotherStore).build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);

        assertThrows(NotFoundException.class, () -> franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal));
    }

    @Test
    void confirmReceiptByOrderId_shouldThrowWhenDeliveryNotFound() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);

        assertThrows(NotFoundException.class, () -> franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal));
    }

    @Test
    void confirmReceiptByOrderId_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User userWithNoStore = new User();
        userWithNoStore.setUsername("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        userWithNoStore.setRole(role);
        userWithNoStore.setStore(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithNoStore));

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal));
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
    void getOrderTimeline_shouldReturnAllTimelineFieldsForCancelledOrder() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.CANCELLED)
                .createdAt(LocalDateTime.now().minusDays(5))
                .assignedAt(LocalDateTime.now().minusDays(4))
                .inProgressAt(LocalDateTime.now().minusDays(3))
                .packedWaitingShipperAt(LocalDateTime.now().minusDays(2))
                .shippingAt(LocalDateTime.now().minusDays(1))
                .cancelledAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        OrderTimelineResponse response = franchiseStoreService.getOrderTimeline("ORD001", principal);

        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getAssignedAt());
        assertNotNull(response.getInProgressAt());
        assertNotNull(response.getPackedWaitingShipperAt());
        assertNotNull(response.getShippingAt());
        assertNotNull(response.getCancelledAt());
        assertNull(response.getDeliveredAt());
    }

    @Test
    void getOrderTimeline_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User userWithNoStore = new User();
        userWithNoStore.setUsername("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        userWithNoStore.setRole(role);
        userWithNoStore.setStore(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithNoStore));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOrderTimeline("ORD001", principal));
    }

    @Test
    void getOrderTimeline_shouldReturnTimelineWithAllFields() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .assignedAt(LocalDateTime.now())
                .inProgressAt(LocalDateTime.now())
                .packedWaitingShipperAt(LocalDateTime.now())
                .shippingAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .cancelledAt(null)
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        OrderTimelineResponse response = franchiseStoreService.getOrderTimeline("ORD001", principal);

        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getAssignedAt());
        assertNotNull(response.getInProgressAt());
        assertNotNull(response.getPackedWaitingShipperAt());
        assertNotNull(response.getShippingAt());
        assertNotNull(response.getDeliveredAt());
        assertNull(response.getCancelledAt());
    }

    @Test
    void getOrderTimeline_shouldThrowWhenPrincipalIsNull() {
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOrderTimeline("ORD001", null));
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
    }

    @Test
    void getDeliveries_shouldReturnAllWhenStatusIsNull() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("ASSIGNED").build();
        Page<Delivery> deliveryPage = new PageImpl<>(List.of(delivery));

        when(deliveryRepository.findByOrder_Store_Id(eq("ST001"), any(PageRequest.class)))
                .thenReturn(deliveryPage);

        Page<DeliveryResponse> result = franchiseStoreService.getDeliveries(null, principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getDeliveries_shouldReturnAllWhenStatusIsBlank() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("ASSIGNED").build();
        Page<Delivery> deliveryPage = new PageImpl<>(List.of(delivery));

        when(deliveryRepository.findByOrder_Store_Id(eq("ST001"), any(PageRequest.class)))
                .thenReturn(deliveryPage);

        Page<DeliveryResponse> result = franchiseStoreService.getDeliveries("   ", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getDeliveries_shouldThrowWhenNotStoreStaff() {
        when(principal.getName()).thenReturn("testuser");
        User nonStaffUser = new User();
        nonStaffUser.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(nonStaffUser));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getDeliveries(null, principal, 0, 10));
    }

    @Test
    void getDeliveries_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User adminUser = new User();
        adminUser.setUsername("testuser");
        Role role = Role.builder().name("ADMIN").build();
        adminUser.setRole(role);
        adminUser.setStore(store);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getDeliveries(null, principal, 0, 10));
    }

    @Test
    void getDeliveries_shouldThrowWhenPrincipalIsNull() {
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getDeliveries(null, null, 0, 10));
    }

    @Test
    void getStoreInventory_shouldReturnInventory() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory(null, null, principal, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLowStock());
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
        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
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
    void getStoreInventory_shouldHandleEmptyResults() {
        mockCurrentStore("testuser", store);
        Page<StoreInventory> emptyPage = new PageImpl<>(Collections.emptyList());

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory(null, null, principal, 0, 10);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldFilterByProductNameOnly() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory(null, "Product", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldFilterByProductIdOnly() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));

        when(productRepository.existsById("PROD001")).thenReturn(true);
        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory("PROD001", null, principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldHandleEmptyProductName() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory(null, "", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldHandleEmptyProductId() {
        mockCurrentStore("testuser", store);
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory("", "Product", principal, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStoreInventory_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User adminUser = new User();
        adminUser.setUsername("testuser");
        Role role = Role.builder().name("ADMIN").build();
        adminUser.setRole(role);
        adminUser.setStore(store);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getStoreInventory(null, null, principal, 0, 10));
    }

    @Test
    void getStoreInventory_shouldThrowWhenPrincipalIsNull() {
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getStoreInventory(null, null, null, 0, 10));
    }

    @Test
    void getAvailableProducts_shouldReturnPagedProducts() {
        Product product2 = Product.builder().id("PROD002").name("Product 2").build();
        Page<Product> productPage = new PageImpl<>(List.of(product, product2));

        when(productRepository.searchProducts(anyString(), any(), any(PageRequest.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts("Product", "BAKERY", 0, 10);

        assertEquals(2, result.getTotalElements());
        verify(productRepository).searchProducts(eq("Product"), any(), any(PageRequest.class));
    }

    @Test
    void getAvailableProducts_shouldThrowWithInvalidCategory() {
        assertThrows(BadRequestException.class, () -> franchiseStoreService.getAvailableProducts(null, "INVALID_CATEGORY", 0, 10));
    }

    @Test
    void getAvailableProducts_shouldHandleNullNameAndCategory() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchProducts(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts(null, null, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableProducts_shouldHandleEmptyNameAndCategory() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchProducts(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts("", "", 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableProducts_shouldHandleValidCategoryOnly() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchProducts(isNull(), eq(ProductCategory.BAKERY), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts(null, "BAKERY", 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableProducts_shouldHandleValidCategoryWithTrim() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchProducts(isNull(), eq(ProductCategory.BAKERY), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts(null, "  BAKERY  ", 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableProducts_shouldHandleNameWithTrim() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchProducts(eq("Product"), isNull(), any(PageRequest.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mock(com.example.demologin.dto.response.ProductResponse.class));

        Page<com.example.demologin.dto.response.ProductResponse> result = franchiseStoreService.getAvailableProducts("  Product  ", null, 0, 10);
        assertEquals(1, result.getTotalElements());
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
    void getMyStore_shouldThrowWhenUserRoleIsNotStoreStaff() {
        when(principal.getName()).thenReturn("testuser");
        User adminUser = new User();
        adminUser.setUsername("testuser");
        Role role = Role.builder().name("ADMIN").build();
        adminUser.setRole(role);
        adminUser.setStore(store);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(adminUser));

        assertThrows(NotFoundException.class, () -> franchiseStoreService.getMyStore(principal));
    }

    @Test
    void getMyStore_shouldThrowWhenPrincipalIsNull() {
        assertThrows(NotFoundException.class, () -> franchiseStoreService.getMyStore(null));
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
    void getOverview_shouldHandleZeroOrders() {
        mockCurrentStore("testuser", store);

        when(orderRepository.countByStore_Id("ST001")).thenReturn(0L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.PENDING)).thenReturn(0L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.IN_PROGRESS)).thenReturn(0L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.SHIPPING)).thenReturn(0L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.DELIVERED)).thenReturn(0L);
        when(orderRepository.countByStore_IdAndStatus("ST001", OrderStatus.CANCELLED)).thenReturn(0L);
        when(storeInventoryRepository.countLowStockItemsByStoreId("ST001")).thenReturn(0L);
        when(deliveryRepository.countByOrder_Store_IdAndStatusIn(eq("ST001"), anyCollection())).thenReturn(0L);

        StoreOverviewResponse overview = franchiseStoreService.getOverview(principal);

        assertEquals(0L, overview.getTotalOrders());
        assertEquals(0L, overview.getActiveDeliveries());
        assertEquals(0L, overview.getLowStockItems());
    }

    @Test
    void getOverview_shouldThrowWhenCurrentStoreIsNull() {
        when(principal.getName()).thenReturn("testuser");
        User userWithNoStore = new User();
        userWithNoStore.setUsername("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        userWithNoStore.setRole(role);
        userWithNoStore.setStore(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithNoStore));

        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOverview(principal));
    }

    @Test
    void getOverview_shouldThrowWhenPrincipalIsNull() {
        assertThrows(IllegalStateException.class, () -> franchiseStoreService.getOverview(null));
    }

    @Test
    void getOrderStatuses_shouldReturnAllEnumStatuses() {
        List<String> statuses = franchiseStoreService.getOrderStatuses();
        assertEquals(OrderStatus.values().length, statuses.size());
        assertTrue(statuses.contains("PENDING"));
        assertTrue(statuses.contains("DELIVERED"));
    }

    @Test
    void getCurrentStore_shouldThrowExceptionWhenUserHasNoStore() {
        when(principal.getName()).thenReturn("testuser");
        Role role = Role.builder().name("FRANCHISE_STORE_STAFF").build();
        User user = new User();
        user.setUsername("testuser");
        user.setRole(role);
        user.setStore(null);
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
    void toDeliveryResponse_shouldHandleNullCoordinatorAndShipper() {
        mockCurrentStore("testuser", store);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("SHIPPING")
                .coordinator(null)
                .shipper(null)
                .build();

        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(request.getReceiverName()).thenReturn("Receiver");
        when(request.getTemperatureOk()).thenReturn(true);

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse response = franchiseStoreService.confirmReceipt("DEL001", request);
        assertNull(response.getCoordinatorName());
        assertNull(response.getShipperName());
    }

    @Test
    void toInventoryResponse_shouldSetLowStockCorrectly() {
        mockCurrentStore("testuser", store);

        StoreInventory inventoryLow = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> pageLow = new PageImpl<>(List.of(inventoryLow));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pageLow);

        Page<StoreInventoryResponse> resultLow = franchiseStoreService.getStoreInventory(null, null, principal, 0, 10);
        assertTrue(resultLow.getContent().get(0).isLowStock());

        StoreInventory inventoryOk = StoreInventory.builder()
                .id(2)
                .store(store)
                .product(product)
                .quantity(15)
                .minStock(10)
                .build();
        Page<StoreInventory> pageOk = new PageImpl<>(List.of(inventoryOk));

        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pageOk);

        Page<StoreInventoryResponse> resultOk = franchiseStoreService.getStoreInventory(null, null, principal, 0, 10);
        assertFalse(resultOk.getContent().get(0).isLowStock());
    }

    @Test
    void getStoreInventory_shouldExecuteSpecificationLambdaForCoverage() {
        mockCurrentStore("testuser", store);

        when(productRepository.existsById("PROD001")).thenReturn(true);
        when(storeInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        franchiseStoreService.getStoreInventory("PROD001", "product", principal, 0, 10);

        verify(storeInventoryRepository, times(1)).findAll(any(Specification.class), any(PageRequest.class));
    }
}