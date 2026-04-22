package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderPickupQrResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Role;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.DeliveryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplyCoordinatorServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private KitchenRepository kitchenRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupplyCoordinatorServiceImpl service;

    private Principal principal;
    private Store store;
    private Kitchen kitchen;
    private User coordinator;
    private Product product;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        store = Store.builder().id("ST001").name("Store 1").build();
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();

        coordinator = new User();
        coordinator.setUserId(10L);
        coordinator.setUsername("supply");
        coordinator.setFullName("Supply User");
        Role role = new Role();
        role.setName("SUPPLY_COORDINATOR");
        coordinator.setRole(role);

        product = new Product();
        product.setId("PROD001");
        product.setName("Test Product");
        product.setUnit("kg");
    }

    private void mockCurrentSupplyCoordinator() {
        when(principal.getName()).thenReturn("supply");
        when(userRepository.findByUsername("supply")).thenReturn(Optional.of(coordinator));
    }

    private void mockDeliveryRepositoryCount(long count) {
        when(deliveryRepository.count()).thenReturn(count);
    }

    private OrderItem createOrderItem() {
        return OrderItem.builder()
                .id(1)
                .product(product)
                .quantity(10)
                .unit("kg")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getOrders() TESTS ====================

    @Test
    void getOrders_shouldThrowWhenDateRangeInvalid() {
        mockCurrentSupplyCoordinator();

        assertThrows(BadRequestException.class,
                () -> service.getOrders(null, null, null, null,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), 0, 20, principal));
    }

    @Test
    void getOrders_shouldThrowWhenStoreNotFound() {
        mockCurrentSupplyCoordinator();
        when(storeRepository.existsById("ST404")).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.getOrders(null, null, "ST404", null,
                        null, null, 0, 20, principal));
    }

    @Test
    void getOrders_shouldThrowWhenKitchenNotFound() {
        mockCurrentSupplyCoordinator();
        when(kitchenRepository.existsById("KIT404")).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.getOrders(null, null, null, "KIT404",
                        null, null, 0, 20, principal));
    }

    @Test
    void getOrders_shouldThrowWhenInvalidStatus() {
        mockCurrentSupplyCoordinator();

        assertThrows(BadRequestException.class,
                () -> service.getOrders("INVALID_STATUS", null, null, null,
                        null, null, 0, 20, principal));
    }

    @Test
    void getOrders_shouldReturnOrdersWithFilters() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders("PENDING", null, "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals("ORD001", response.getContent().get(0).getId());
    }

    @Test
    void getOrders_WithPriorityFilter_ReturnsFilteredOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("HIGH").build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, "HIGH", "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithOnlyFromDate_ReturnsFilteredOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, "ST001", null,
                LocalDate.of(2026, 4, 1), null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithOnlyToDate_ReturnsFilteredOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, "ST001", null,
                null, LocalDate.of(2026, 4, 30), 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithKitchenFilter_ReturnsFilteredOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(kitchenRepository.existsById("KIT001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, null, "KIT001", null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals("ORD001", response.getContent().get(0).getId());
    }

    @Test
    void getOrders_WithAllFilters_ReturnsFilteredOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .kitchen(kitchen)
                .status(OrderStatus.ASSIGNED)
                .priority("HIGH")
                .build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(kitchenRepository.existsById("KIT001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(
                "ASSIGNED", "HIGH", "ST001", "KIT001",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithOrderItems_ReturnsOrderResponseWithItems() {
        mockCurrentSupplyCoordinator();

        OrderItem orderItem = createOrderItem();
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        Page<OrderResponse> response = service.getOrders("PENDING", null, "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().get(0).getItems().size());
        assertEquals("PROD001", response.getContent().get(0).getItems().get(0).getProductId());
        assertEquals("Test Product", response.getContent().get(0).getItems().get(0).getProductName());
    }

    // ==================== getOrderById() TESTS ====================

    @Test
    void getOrderById_WhenOrderExists_ReturnsOrderResponse() {
        mockCurrentSupplyCoordinator();
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.getOrderById("ORD001", principal);

        assertNotNull(response);
        assertEquals("ORD001", response.getId());
    }

    @Test
    void getOrderById_WhenOrderNotFound_ThrowsNotFoundException() {
        mockCurrentSupplyCoordinator();
        when(orderRepository.findById("ORD999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getOrderById("ORD999", principal));
    }

    @Test
    void getOrderById_WhenStoreIsNull_ReturnsResponseWithoutStoreInfo() {
        mockCurrentSupplyCoordinator();
        Order order = Order.builder()
                .id("ORD001")
                .store(null)
                .kitchen(null)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.getOrderById("ORD001", principal);

        assertNull(response.getStoreId());
        assertNull(response.getStoreName());
        assertNull(response.getKitchenId());
        assertNull(response.getKitchenName());
    }

    @Test
    void getOrderById_WithOrderItems_ReturnsOrderResponseWithItems() {
        mockCurrentSupplyCoordinator();

        OrderItem orderItem = createOrderItem();
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        OrderResponse response = service.getOrderById("ORD001", principal);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("PROD001", response.getItems().get(0).getProductId());
        assertEquals("Test Product", response.getItems().get(0).getProductName());
        assertEquals(10, response.getItems().get(0).getQuantity());
        assertEquals("kg", response.getItems().get(0).getUnit());
    }

    // ==================== assignOrderToKitchen() TESTS ====================

    @Test
    void assignOrderToKitchen_shouldAssignAndAppendNote() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");
        ReflectionTestUtils.setField(request, "notes", "Manual assignment");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1))
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertEquals("KIT001", response.getKitchenId());
        assertNotNull(order.getAssignedAt());
        assertTrue(response.getNotes().contains("Manual assignment"));
    }

    @Test
    void assignOrderToKitchen_WhenOrderAlreadyAssigned_StillUpdatesKitchen() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT002");

        Kitchen newKitchen = Kitchen.builder().id("KIT002").name("Kitchen 2").build();

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT002")).thenReturn(Optional.of(newKitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertEquals("KIT002", response.getKitchenId());
        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
    }

    @Test
    void assignOrderToKitchen_WhenOrderCompleted_ThrowsBadRequestException() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));

        assertThrows(BadRequestException.class,
                () -> service.assignOrderToKitchen("ORD001", request, principal));
    }

    @Test
    void assignOrderToKitchen_WhenNotesNull_DoesNotAppendNote() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");
        ReflectionTestUtils.setField(request, "notes", null);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .notes(null)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertNull(response.getNotes());
    }

    @Test
    void assignOrderToKitchen_WithOrderItems_ReturnsOrderResponseWithItems() {
        mockCurrentSupplyCoordinator();

        OrderItem orderItem = createOrderItem();
        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertEquals(1, response.getItems().size());
        assertEquals("PROD001", response.getItems().get(0).getProductId());
    }

    // ==================== getOverview() TESTS ====================

    @Test
    void getOverview_shouldReturnAggregatedValues() {
        mockCurrentSupplyCoordinator();

        when(orderRepository.count(any(Specification.class)))
                .thenReturn(120L, 20L, 30L, 10L, 8L, 7L, 40L, 5L, 3L, 2L);
        when(deliveryRepository.countByCoordinator_UserIdAndStatusIn(anyLong(), any()))
                .thenReturn(9L);

        SupplyCoordinatorOverviewResponse response = service.getOverview(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), principal
        );

        assertEquals(120L, response.getTotalOrders());
        assertEquals(20L, response.getPendingOrders());
        assertEquals(30L, response.getAssignedOrders());
        assertEquals(10L, response.getInProgressOrders());
        assertEquals(8L, response.getPackedWaitingShipperOrders());
        assertEquals(7L, response.getShippingOrders());
        assertEquals(40L, response.getDeliveredOrders());
        assertEquals(5L, response.getCancelledOrders());
        assertEquals(3L, response.getOverdueOrders());
        assertEquals(2L, response.getUnassignedOrders());
        assertEquals(9L, response.getActiveDeliveries());

        verify(deliveryRepository).countByCoordinator_UserIdAndStatusIn(coordinator.getUserId(), List.of("ASSIGNED", "SHIPPING", "DELAYED"));
    }

    @Test
    void getOverview_WhenDatesNull_UsesCurrentDate() {
        mockCurrentSupplyCoordinator();

        when(orderRepository.count(any(Specification.class)))
                .thenReturn(10L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        when(deliveryRepository.countByCoordinator_UserIdAndStatusIn(anyLong(), any()))
                .thenReturn(9L);

        SupplyCoordinatorOverviewResponse response = service.getOverview(null, null, principal);

        assertNotNull(response);
        verify(orderRepository, atLeastOnce()).count(any(Specification.class));
    }

    // ==================== scheduleDelivery() TESTS ====================

    @Test
    void scheduleDelivery_shouldCreateShippingDeliveryAndUpdateOrder() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(12L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "SHIPPING");
        ReflectionTestUtils.setField(request, "notes", "Ship now");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertEquals("SHIPPING", response.getStatus());
        assertEquals("ORD001", response.getOrderId());
        assertEquals("Supply User", response.getCoordinatorName());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void scheduleDelivery_WithAssignedStatus_DoesNotChangeOrderToShipping() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(0L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertEquals("ASSIGNED", response.getStatus());
        assertEquals(OrderStatus.ASSIGNED, order.getStatus());
        assertNull(order.getShippingAt());
    }

    @Test
    void scheduleDelivery_WhenStatusIsNull_DefaultsToAssigned() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(0L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", null);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertEquals("ASSIGNED", response.getStatus());
        assertEquals(OrderStatus.ASSIGNED, order.getStatus());
    }

    @Test
    void scheduleDelivery_WithCustomAssignedAt_UsesProvidedTime() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(12L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");
        ReflectionTestUtils.setField(request, "assignedAt", LocalDateTime.of(2026, 4, 21, 10, 0));

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertNotNull(response);
        verify(deliveryRepository).save(argThat(delivery ->
                delivery.getAssignedAt().equals(LocalDateTime.of(2026, 4, 21, 10, 0))
        ));
    }

    @Test
    void scheduleDelivery_shouldThrowWhenDeliveryAlreadyExists() {
        mockCurrentSupplyCoordinator();

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery existing = Delivery.builder().id("DEL001").order(order).status("ASSIGNED").build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> service.scheduleDelivery(request, principal));
    }

    @Test
    void scheduleDelivery_shouldThrowWhenOrderCancelled() {
        mockCurrentSupplyCoordinator();

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> service.scheduleDelivery(request, principal));
    }

    @Test
    void scheduleDelivery_shouldThrowWhenInvalidStatus() {
        mockCurrentSupplyCoordinator();

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "INVALID_STATUS");

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.scheduleDelivery(request, principal));
    }

    @Test
    void scheduleDelivery_WhenOrderHasAssignedAt_DoesNotOverride() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(12L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");

        LocalDateTime existingAssignedAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .assignedAt(existingAssignedAt)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        service.scheduleDelivery(request, principal);

        assertEquals(existingAssignedAt, order.getAssignedAt());
    }

    // ==================== getDeliveries() TESTS ====================

    @Test
    void getDeliveries_shouldThrowWhenStatusInvalid() {
        mockCurrentSupplyCoordinator();

        assertThrows(BadRequestException.class, () -> service.getDeliveries("WRONG", 0, 20, principal));
    }

    @Test
    void getDeliveries_shouldReturnAllDeliveriesWhenStatusNull() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).store(store).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findByCoordinator_UserId(anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries(null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals("DEL001", response.getContent().get(0).getId());
    }

    @Test
    void getDeliveries_shouldReturnCoordinatorScopedPage() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).store(store).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findByCoordinator_UserIdAndStatus(anyLong(), eq("SHIPPING"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries("SHIPPING", 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals("DEL001", response.getContent().get(0).getId());
        assertEquals("SHIPPING", response.getContent().get(0).getStatus());
    }

    @Test
    void getDeliveries_WhenCoordinatorIsNull_ReturnsResponseWithoutCoordinatorName() {
        mockCurrentSupplyCoordinator();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .coordinator(null)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        Order order = Order.builder().id("ORD001").build();
        delivery.setOrder(order);

        when(deliveryRepository.findByCoordinator_UserId(anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries(null, 0, 20, principal);

        assertNull(response.getContent().get(0).getCoordinatorName());
    }

    // ==================== updateDeliveryStatus() TESTS ====================

    @Test
    void updateDeliveryStatus_shouldMarkDelivered() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "DELIVERED");
        ReflectionTestUtils.setField(request, "receiverName", "Store Staff");
        ReflectionTestUtils.setField(request, "temperatureOk", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("DELIVERED", response.getStatus());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertNotNull(order.getDeliveredAt());
        assertNotNull(response.getDeliveredAt());
        assertEquals("Store Staff", response.getReceiverName());
        assertTrue(response.getTemperatureOk());
    }

    @Test
    void updateDeliveryStatus_WhenStatusShipping_UpdatesOrderShippingAt() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "SHIPPING");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("SHIPPING", response.getStatus());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void updateDeliveryStatus_WhenStatusAssigned_UpdatesOrderAssignedAt() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .assignedAt(null)
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("ASSIGNED", response.getStatus());
        assertEquals(OrderStatus.ASSIGNED, order.getStatus());
        assertNotNull(order.getAssignedAt());
    }

    @Test
    void updateDeliveryStatus_WhenStatusCancelled_UpdatesOrderCancelledAt() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "CANCELLED");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("CANCELLED", response.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void updateDeliveryStatus_WhenStatusDelayed_AddsNoteToOrder() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "DELAYED");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .notes(null)
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("DELAYED", response.getStatus());
        assertTrue(order.getNotes().contains("Delivery delayed"));
    }

    @Test
    void updateDeliveryStatus_WhenStatusWaitingConfirm_UpdatesCorrectly() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "WAITING_CONFIRM");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("WAITING_CONFIRM", response.getStatus());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
    }

    @Test
    void updateDeliveryStatus_WithNotes_AppendsNote() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "SHIPPING");
        ReflectionTestUtils.setField(request, "notes", "Updated by coordinator");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .notes("Old note")
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertTrue(delivery.getNotes().contains("Old note"));
        assertTrue(delivery.getNotes().contains("Updated by coordinator"));
    }

    @Test
    void updateDeliveryStatus_WhenStatusMissing_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", null);

        Delivery delivery = Delivery.builder().id("DEL001").build();
        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class,
                () -> service.updateDeliveryStatus("DEL001", request, principal));
    }

    @Test
    void updateDeliveryStatus_WhenStatusInvalid_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "INVALID_STATUS");

        Delivery delivery = Delivery.builder().id("DEL001").build();
        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class,
                () -> service.updateDeliveryStatus("DEL001", request, principal));
    }

    @Test
    void updateDeliveryStatus_WhenOrderAssignedAtAlreadyExists_DoesNotOverride() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");

        LocalDateTime existingAssignedAt = LocalDateTime.now().minusDays(2);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .assignedAt(existingAssignedAt)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals(existingAssignedAt, order.getAssignedAt());
    }

    @Test
    void updateDeliveryStatus_WhenOrderShippingAtAlreadyExists_DoesNotOverride() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "SHIPPING");

        LocalDateTime existingShippingAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .shippingAt(existingShippingAt)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals(existingShippingAt, order.getShippingAt());
    }

    @Test
    void updateDeliveryStatus_WhenDeliveryDeliveredAtAlreadyExists_DoesNotOverride() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "DELIVERED");

        LocalDateTime existingDeliveredAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .deliveredAt(existingDeliveredAt)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .deliveredAt(existingDeliveredAt)
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals(existingDeliveredAt, delivery.getDeliveredAt());
        assertEquals(existingDeliveredAt, order.getDeliveredAt());
    }

    @Test
    void updateDeliveryStatus_WhenOrderCancelledAtAlreadyExists_DoesNotOverride() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "CANCELLED");

        LocalDateTime existingCancelledAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .cancelledAt(existingCancelledAt)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals(existingCancelledAt, order.getCancelledAt());
    }

    @Test
    void updateDeliveryStatus_WithReceiverNameAndTemperatureOk_UpdatesFields() {
        mockCurrentSupplyCoordinator();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "DELIVERED");
        ReflectionTestUtils.setField(request, "receiverName", "John Doe");
        ReflectionTestUtils.setField(request, "temperatureOk", false);
        ReflectionTestUtils.setField(request, "notes", "Delivered with care");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertEquals("John Doe", response.getReceiverName());
        assertFalse(response.getTemperatureOk());
        assertTrue(delivery.getNotes().contains("Delivered with care"));
    }

    // ==================== handleIssue() TESTS ====================

    @Test
    void handleIssue_shouldDelayDeliveryWhenIssueTypeDelay() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "DELAY");
        ReflectionTestUtils.setField(request, "description", "Traffic jam");
        ReflectionTestUtils.setField(request, "cancelOrder", false);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.SHIPPING, response.getStatus());
        assertEquals("DELAYED", delivery.getStatus());
        assertTrue(response.getNotes().contains("[DELAY] Traffic jam"));
    }

    @Test
    void handleIssue_shouldCancelOrderAndDelivery() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "CANCELLATION");
        ReflectionTestUtils.setField(request, "description", "Store cancelled");
        ReflectionTestUtils.setField(request, "cancelOrder", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("CANCELLED", delivery.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void handleIssue_WithShortageIssue_OnlyAddsNoteWithoutCancelling() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "SHORTAGE");
        ReflectionTestUtils.setField(request, "description", "Missing 5 boxes");
        ReflectionTestUtils.setField(request, "cancelOrder", false);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertTrue(response.getNotes().contains("SHORTAGE"));
        assertTrue(response.getNotes().contains("Missing 5 boxes"));
    }

    @Test
    void handleIssue_WithOtherIssueType_OnlyAddsNote() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "OTHER");
        ReflectionTestUtils.setField(request, "description", "Customer requested change");
        ReflectionTestUtils.setField(request, "cancelOrder", false);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.IN_PROGRESS, response.getStatus());
        assertTrue(response.getNotes().contains("OTHER"));
    }

    @Test
    void handleIssue_WithDelayButNoDelivery_OnlyAddsNote() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "DELAY");
        ReflectionTestUtils.setField(request, "description", "Delay reason");
        ReflectionTestUtils.setField(request, "cancelOrder", false);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertTrue(response.getNotes().contains("DELAY"));
    }

    @Test
    void handleIssue_WithDelayAndDeliveryAlreadyDelivered_DoesNotChangeDelivery() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "DELAY");
        ReflectionTestUtils.setField(request, "description", "Delay after delivery");
        ReflectionTestUtils.setField(request, "cancelOrder", false);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("DELIVERED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        assertEquals("DELIVERED", delivery.getStatus());
    }

    @Test
    void handleIssue_WithCancelOrderTrue_CancelsEvenIfNotCancellationType() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "DELAY");
        ReflectionTestUtils.setField(request, "description", "Too long delay");
        ReflectionTestUtils.setField(request, "cancelOrder", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.SHIPPING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("CANCELLED", delivery.getStatus());
    }

    @Test
    void handleIssue_WithCancellationAndDeliveryAlreadyDelivered_StillCancelsOrder() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "CANCELLATION");
        ReflectionTestUtils.setField(request, "description", "Cancel after delivery");
        ReflectionTestUtils.setField(request, "cancelOrder", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .status("DELIVERED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
    }

    @Test
    void handleIssue_WithCancellationAndDeliveryIsNull_CancelsOrderOnly() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "CANCELLATION");
        ReflectionTestUtils.setField(request, "description", "Cancel before delivery created");
        ReflectionTestUtils.setField(request, "cancelOrder", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id(anyString())).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void handleIssue_WhenIssueTypeMissing_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "description", "Some issue");
        ReflectionTestUtils.setField(request, "issueType", null);

        Order order = Order.builder().id("ORD001").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> service.handleIssue("ORD001", request, principal));
    }

    @Test
    void handleIssue_WhenDescriptionMissing_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "DELAY");
        ReflectionTestUtils.setField(request, "description", null);

        Order order = Order.builder().id("ORD001").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> service.handleIssue("ORD001", request, principal));
    }

    @Test
    void handleIssue_WhenUnsupportedIssueType_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "UNSUPPORTED");
        ReflectionTestUtils.setField(request, "description", "Something");

        Order order = Order.builder().id("ORD001").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> service.handleIssue("ORD001", request, principal));
    }

    @Test
    void handleIssue_WithCancellation_ReturnsOrderResponseWithItems() {
        mockCurrentSupplyCoordinator();

        OrderItem orderItem = createOrderItem();
        HandleIssueRequest request = new HandleIssueRequest();
        ReflectionTestUtils.setField(request, "issueType", "CANCELLATION");
        ReflectionTestUtils.setField(request, "description", "Store cancelled");
        ReflectionTestUtils.setField(request, "cancelOrder", true);

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(orderItem));

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("PROD001", response.getItems().get(0).getProductId());
    }

    // ==================== getOrderPickupQr() TESTS ====================

    @Test
    void getOrderPickupQr_WhenDeliveryExists_ReturnsQr() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").pickupQrCode("QR123").build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));

        OrderPickupQrResponse response = service.getOrderPickupQr("ORD001", principal);

        assertEquals("QR123", response.getPickupQrCode());
        assertEquals("ORD001", response.getOrderId());
        assertEquals("DEL001", response.getDeliveryId());
    }

    @Test
    void getOrderPickupQr_WhenDeliveryNotExistsButStatusValid_CreatesDelivery() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(5L);

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.PACKED_WAITING_SHIPPER)
                .store(store)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPickupQrResponse response = service.getOrderPickupQr("ORD001", principal);

        assertNotNull(response.getPickupQrCode());
        assertTrue(response.getPickupQrCode().startsWith("PK-ORD001-"));
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void getOrderPickupQr_WhenDeliveryExistsButQrNull_GeneratesNewQr() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").pickupQrCode(null).build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPickupQrResponse response = service.getOrderPickupQr("ORD001", principal);

        assertNotNull(response.getPickupQrCode());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void getOrderPickupQr_WhenDeliveryExistsAndQrCodeBlank_GeneratesNewQr() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder().id("DEL001").pickupQrCode("").build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPickupQrResponse response = service.getOrderPickupQr("ORD001", principal);

        assertNotNull(response.getPickupQrCode());
        assertTrue(response.getPickupQrCode().startsWith("PK-ORD001-"));
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void getOrderPickupQr_WhenOrderStatusInvalidForPickup_ThrowsBadRequest() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.PENDING)
                .store(store)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.getOrderPickupQr("ORD001", principal));
    }

    @Test
    void getOrderPickupQr_WhenOrderStatusShipping_CreatesDeliveryWithShippingStatus() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(5L);

        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.SHIPPING)
                .store(store)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPickupQrResponse response = service.getOrderPickupQr("ORD001", principal);

        assertNotNull(response);
        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        assertEquals("SHIPPING", deliveryCaptor.getValue().getStatus());
    }

    // ==================== getOrderHolder() TESTS ====================

    @Test
    void getOrderHolder_WhenDeliveryExists_ReturnsHolderResponse() {
        mockCurrentSupplyCoordinator();

        User shipper = new User();
        shipper.setUserId(1L);
        shipper.setUsername("shipper1");
        shipper.setFullName("Shipper One");

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .shipper(shipper)
                .status("SHIPPING")
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));

        OrderHolderResponse response = service.getOrderHolder("ORD001", principal);

        assertEquals("ORD001", response.getOrderId());
        assertEquals("DEL001", response.getDeliveryId());
        assertEquals(1L, response.getHolderUserId());
        assertEquals("shipper1", response.getHolderUsername());
        assertEquals("Shipper One", response.getHolderFullName());
    }

    @Test
    void getOrderHolder_WhenDeliveryNotFound_ThrowsNotFoundException() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getOrderHolder("ORD001", principal));
    }

    @Test
    void getOrderHolder_WhenShipperIsNull_ReturnsResponseWithoutShipperInfo() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .shipper(null)
                .status("SHIPPING")
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));

        OrderHolderResponse response = service.getOrderHolder("ORD001", principal);

        assertNull(response.getHolderUserId());
        assertNull(response.getHolderUsername());
        assertNull(response.getHolderFullName());
    }

    // ==================== getOrderStatuses() TESTS ====================

    @Test
    void getOrderStatuses_shouldRequireSupplyCoordinator() {
        when(principal.getName()).thenReturn("manager");
        User manager = new User();
        manager.setUsername("manager");
        Role role = new Role();
        role.setName("MANAGER");
        manager.setRole(role);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThrows(IllegalStateException.class, () -> service.getOrderStatuses(principal));
        verify(orderRepository, never()).findById(anyString());
    }

    @Test
    void getOrderStatuses_WhenValidCoordinator_ReturnsAllStatuses() {
        mockCurrentSupplyCoordinator();

        List<String> statuses = service.getOrderStatuses(principal);

        assertEquals(7, statuses.size());
        assertTrue(statuses.containsAll(List.of(
                "PENDING", "ASSIGNED", "IN_PROGRESS",
                "PACKED_WAITING_SHIPPER", "SHIPPING", "DELIVERED", "CANCELLED")));
    }

    // ==================== getDeliveryStatuses() TESTS ====================

    @Test
    void getDeliveryStatuses_ReturnsAllStatuses() {
        mockCurrentSupplyCoordinator();

        List<String> statuses = service.getDeliveryStatuses(principal);

        assertTrue(statuses.containsAll(List.of(
                "ASSIGNED", "SHIPPING", "DELAYED", "WAITING_CONFIRM", "DELIVERED", "CANCELLED")));
    }

    // ==================== Authentication / Authorization TESTS ====================

    @Test
    void whenPrincipalIsNull_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> service.getOrderStatuses(null));
    }

    @Test
    void whenUserNotFound_ThrowsNotFoundException() {
        when(principal.getName()).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getOrderStatuses(principal));
    }

    @Test
    void whenUserHasWrongRole_ThrowsIllegalStateException() {
        when(principal.getName()).thenReturn("manager");
        User manager = new User();
        manager.setUsername("manager");
        Role role = new Role();
        role.setName("STORE_MANAGER");
        manager.setRole(role);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThrows(IllegalStateException.class,
                () -> service.getOrderStatuses(principal));
    }

    // ==================== generateDeliveryId() Format Test ====================

    @Test
    void generateDeliveryId_ShouldHaveCorrectFormat() {
        mockCurrentSupplyCoordinator();
        mockDeliveryRepositoryCount(0L);

        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest();
        ReflectionTestUtils.setField(request, "orderId", "ORD001");
        ReflectionTestUtils.setField(request, "status", "ASSIGNED");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertNotNull(response.getId());
        assertEquals(10, response.getId().length());
        assertTrue(response.getId().startsWith("DEL"));
        String afterDel = response.getId().substring(3);
        assertTrue(afterDel.matches("\\d{7}"));
    }

    // ==================== Edge Cases for appendNote() ====================

    @Test
    void appendNote_WhenOldNotesIsNull_CreatesNewNote() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");
        ReflectionTestUtils.setField(request, "notes", "First note");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .notes(null)
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertNotNull(response.getNotes());
        assertTrue(response.getNotes().contains("First note"));
    }

    @Test
    void appendNote_WhenOldNotesIsBlank_CreatesNewNote() {
        mockCurrentSupplyCoordinator();

        AssignOrderKitchenRequest request = new AssignOrderKitchenRequest();
        ReflectionTestUtils.setField(request, "kitchenId", "KIT001");
        ReflectionTestUtils.setField(request, "notes", "First note");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .notes("")
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.assignOrderToKitchen("ORD001", request, principal);

        assertNotNull(response.getNotes());
        assertTrue(response.getNotes().contains("First note"));
    }

    // ==================== Private Methods Coverage ====================

    @Test
    void parseOrderStatus_WhenStatusIsNull_ReturnsNull() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void validateDateRange_WhenFromDateIsNull_DoesNotThrow() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, "ST001", null, null, LocalDate.of(2026, 4, 30), 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void validateDateRange_WhenToDateIsNull_DoesNotThrow() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(null, null, "ST001", null, LocalDate.of(2026, 4, 1), null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }
    // ==================== COVER toDeliveryResponse BRANCHES ====================

    @Test
    void getDeliveries_WhenShipperIsNotNull_ReturnsResponseWithShipperName() {
        mockCurrentSupplyCoordinator();

        User shipper = new User();
        shipper.setUserId(2L);
        shipper.setUsername("shipper01");
        shipper.setFullName("Shipper One");

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).store(store).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .shipper(shipper)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findByCoordinator_UserId(anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries(null, 0, 20, principal);

        assertNotNull(response.getContent().get(0).getShipperName());
        assertEquals("Shipper One", response.getContent().get(0).getShipperName());
    }

    @Test
    void getDeliveries_WhenShipperIsNull_ReturnsResponseWithoutShipperName() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).store(store).build();
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .shipper(null)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findByCoordinator_UserId(anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries(null, 0, 20, principal);

        assertNull(response.getContent().get(0).getShipperName());
    }

    @Test
    void updateDeliveryStatus_WithShipperNotNull_ReturnsResponseWithShipperName() {
        mockCurrentSupplyCoordinator();

        User shipper = new User();
        shipper.setUserId(2L);
        shipper.setUsername("shipper01");
        shipper.setFullName("Shipper One");

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        ReflectionTestUtils.setField(request, "status", "SHIPPING");

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now())
                .build();

        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .shipper(shipper)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById("DEL001")).thenReturn(Optional.of(delivery));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.updateDeliveryStatus("DEL001", request, principal);

        assertNotNull(response.getShipperName());
        assertEquals("Shipper One", response.getShipperName());
    }

    // ==================== COVER normalizeText() BRANCH ====================

    @Test
    void getOrders_WithEmptyStringPriority_ReturnsOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        // priority là empty string -> normalizeText sẽ trả về null
        Page<OrderResponse> response = service.getOrders(null, "", "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithBlankStringPriority_ReturnsOrders() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        // priority là blank string (chỉ spaces) -> normalizeText sẽ trả về null
        Page<OrderResponse> response = service.getOrders(null, "   ", "ST001", null, null, null, 0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    // ==================== COVER requestedDateBetween() BRANCHES ====================

    @Test
    void getOrders_WithFromDateOnly_AppliesFromDateFilter() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .requestedDate(LocalDate.of(2026, 4, 15))
                .build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(
                null, null, "ST001", null,
                LocalDate.of(2026, 4, 1), null,
                0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getOrders_WithToDateOnly_AppliesToDateFilter() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .requestedDate(LocalDate.of(2026, 4, 15))
                .build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        Page<OrderResponse> response = service.getOrders(
                null, null, "ST001", null,
                null, LocalDate.of(2026, 4, 30),
                0, 20, principal);

        assertEquals(1, response.getTotalElements());
    }

    // ==================== COVER hasStatus(), hasStatuses(), kitchenIsNull(), requestedDateBefore() ====================

    // Các method này đã được cover qua getOverview() test
    // hasStatus: pendingOrders, assignedOrders, packedWaitingShipperOrders, shippingOrders, deliveredOrders, cancelledOrders
    // hasStatuses: inProgressOrders (IN_PROGRESS, PROCESSING), overdueOrders (ACTIVE_ORDER_STATUSES)
    // kitchenIsNull: unassignedOrders
    // requestedDateBefore: overdueOrders

    @Test
    void getOverview_VerifyAllSpecificationsAreCalled() {
        mockCurrentSupplyCoordinator();

        when(orderRepository.count(any(Specification.class)))
                .thenReturn(10L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        when(deliveryRepository.countByCoordinator_UserIdAndStatusIn(anyLong(), any()))
                .thenReturn(5L);

        SupplyCoordinatorOverviewResponse response = service.getOverview(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), principal);

        assertNotNull(response);
        // Verify các specification đã được gọi
        verify(orderRepository, atLeast(10)).count(any(Specification.class));
    }
}