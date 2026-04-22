package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.*;
import com.example.demologin.entity.*;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralKitchenServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ProductionPlanRepository productionPlanRepository;
    @Mock private ProductRepository productRepository;
    @Mock private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CentralKitchenServiceImpl centralKitchenService;

    private Principal principal;
    private Kitchen kitchen;
    private Store store;
    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        kitchen = Kitchen.builder().id("KIT001").name("Central Kitchen")
                .address("123 Main St").phone("123456789").capacity(1000).status("ACTIVE").build();
        store = Store.builder().id("ST001").name("Store 1").address("456 Oak St")
                .phone("987654321").manager("John Doe").status("ACTIVE")
                .openDate(LocalDate.now().minusDays(30)).build();
        role = Role.builder().id(1L).name("CENTRAL_KITCHEN_STAFF").build();
    }

    private void mockCentralKitchenUser(String username, Kitchen assignedKitchen) {
        when(principal.getName()).thenReturn(username);
        user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setKitchen(assignedKitchen);
        user.setStatus(com.example.demologin.enums.UserStatus.ACTIVE);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    private UpdateOrderStatusRequest createUpdateStatusRequest(OrderStatus status, String notes) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        try {
            java.lang.reflect.Field sf = UpdateOrderStatusRequest.class.getDeclaredField("status");
            sf.setAccessible(true); sf.set(request, status);
            java.lang.reflect.Field nf = UpdateOrderStatusRequest.class.getDeclaredField("notes");
            nf.setAccessible(true); nf.set(request, notes);
        } catch (Exception e) { throw new RuntimeException(e); }
        return request;
    }

    private CreateProductionPlanRequest createProductionPlanRequest(
            String productId, Integer quantity, LocalDateTime startDate, LocalDateTime endDate, String notes) {
        CreateProductionPlanRequest request = new CreateProductionPlanRequest();
        try {
            java.lang.reflect.Field f1 = CreateProductionPlanRequest.class.getDeclaredField("productId");
            f1.setAccessible(true); f1.set(request, productId);
            java.lang.reflect.Field f2 = CreateProductionPlanRequest.class.getDeclaredField("quantity");
            f2.setAccessible(true); f2.set(request, quantity);
            java.lang.reflect.Field f3 = CreateProductionPlanRequest.class.getDeclaredField("startDate");
            f3.setAccessible(true); f3.set(request, startDate);
            java.lang.reflect.Field f4 = CreateProductionPlanRequest.class.getDeclaredField("endDate");
            f4.setAccessible(true); f4.set(request, endDate);
            java.lang.reflect.Field f5 = CreateProductionPlanRequest.class.getDeclaredField("notes");
            f5.setAccessible(true); f5.set(request, notes);
        } catch (Exception e) { throw new RuntimeException(e); }
        return request;
    }

    // ==================== getOrderById ====================

    @Test
    void getOrderById_shouldReturnOrderResponse_WhenOrderExists() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Product product = Product.builder().id("PROD001").name("Bread").build();
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .notes("Test order").createdBy("customer@test.com").total(BigDecimal.valueOf(100)).build();
        OrderItem item = OrderItem.builder().id(1).order(order).product(product).quantity(5).unit("piece").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(item));

        OrderResponse response = centralKitchenService.getOrderById("ORD001", principal);
        assertNotNull(response);
        assertEquals("ORD001", response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderById_shouldThrowNotFoundException_WhenOrderNotExists() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.findById("NOT_EXIST")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> centralKitchenService.getOrderById("NOT_EXIST", principal));
    }

    @Test
    void getOrderById_shouldHandleNullStoreAndKitchen() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(null).kitchen(null).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1)).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.getOrderById("ORD001", principal);
        assertNull(response.getStoreId());
        assertNull(response.getKitchenId());
    }

    // ==================== assignOrder ====================

    @Test
    void assignOrder_shouldAssignKitchenAndSetAssignedAt_WhenOrderIsPending() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1")
                .createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1)).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.assignOrder("ORD001", principal);
        assertNotNull(response);
        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertEquals("KIT001", response.getKitchenId());
        verify(orderRepository).save(order);
    }

    @Test
    void assignOrder_shouldStillAssign_WhenOrderAlreadyAssigned() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.ASSIGNED).priority("1").kitchen(kitchen).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.assignOrder("ORD001", principal);
        assertNotNull(response);
        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsInProgress() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.IN_PROGRESS).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsDelivered() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.DELIVERED).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsCancelled() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.CANCELLED).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenUserHasNoKitchen() {
        mockCentralKitchenUser("kitchenstaff", null);
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
    }

    @Test
    void assignOrder_shouldNotOverrideExistingAssignedAt() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingAssignedAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1")
                .assignedAt(existingAssignedAt).build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.assignOrder("ORD001", principal);
        assertEquals(existingAssignedAt, order.getAssignedAt());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsShipping() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsPackedWaitingShipper() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.PACKED_WAITING_SHIPPER).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }

    // ==================== updateOrderStatus ====================

    @Test
    void updateOrderStatus_shouldUpdateStatusAndSetTimeline_WhenValidTransition() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes("old-note").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.IN_PROGRESS, "started production");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.IN_PROGRESS, response.getStatus());
        assertNotNull(order.getInProgressAt());
        assertTrue(response.getNotes().contains("started production"));
    }

    @Test
    void updateOrderStatus_shouldThrowBadRequestException_WhenInvalidTransition() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.PENDING).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.DELIVERED, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.updateOrderStatus("ORD001", request, principal));
    }

    @Test
    void updateOrderStatus_shouldOnlyUpdateNotes_WhenStatusSame() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes("old-note").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, "additional note");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertTrue(response.getNotes().contains("additional note"));
    }

    @Test
    void updateOrderStatus_shouldNotAppendNotes_WhenNotesBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes("old-note").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, "   ");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals("old-note", response.getNotes());
    }

    @Test
    void updateOrderStatus_shouldNotAppendNotes_WhenNotesNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes("old-note").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals("old-note", response.getNotes());
    }

    @Test
    void updateOrderStatus_shouldThrowException_WhenFromDelivered() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.DELIVERED).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.CANCELLED, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.updateOrderStatus("ORD001", request, principal));
    }

    @Test
    void updateOrderStatus_shouldThrowException_WhenFromCancelled() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.CANCELLED).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.IN_PROGRESS, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.updateOrderStatus("ORD001", request, principal));
    }

    @Test
    void updateOrderStatus_shouldAllowProcessingToPackedWaitingShipper() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PROCESSING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.PACKED_WAITING_SHIPPER, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.PACKED_WAITING_SHIPPER, response.getStatus());
        assertNotNull(order.getPackedWaitingShipperAt());
    }

    @Test
    void updateOrderStatus_shouldAllowApprovedToInProgress() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.APPROVED)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.IN_PROGRESS, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    void updateOrderStatus_shouldAllowProcessingToShipping() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.PROCESSING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(null).shippingAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.SHIPPING, response.getStatus());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void updateOrderStatus_shouldAllowProcessingToCancel() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.PROCESSING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .cancelledAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.CANCELLED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void updateOrderStatus_shouldAllowApprovedToShipping() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.APPROVED)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(null).shippingAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.SHIPPING, response.getStatus());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void updateOrderStatus_shouldAllowApprovedToCancel() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.APPROVED)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .cancelledAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.CANCELLED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void updateOrderStatus_shouldThrowBadRequest_WhenCurrentStatusHasNoTransitions() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").status(OrderStatus.DELIVERED).priority("1").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.updateOrderStatus("ORD001", request, principal));
    }

    @Test
    void updateOrderStatus_shouldSetAllTimestamps_WhenStatusIsDelivered() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.SHIPPING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(null)
                .shippingAt(null).deliveredAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.DELIVERED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        assertNotNull(order.getAssignedAt());
        assertNotNull(order.getInProgressAt());
        assertNotNull(order.getPackedWaitingShipperAt());
        assertNotNull(order.getShippingAt());
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideExistingTimestamps_WhenTransitionToDelivered() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(3);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.SHIPPING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).inProgressAt(existingTs).packedWaitingShipperAt(existingTs)
                .shippingAt(existingTs).deliveredAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.DELIVERED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getAssignedAt());
        assertEquals(existingTs, order.getInProgressAt());
        assertEquals(existingTs, order.getPackedWaitingShipperAt());
        assertEquals(existingTs, order.getShippingAt());
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideShippingAt_WhenTransitionToDelivered() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(1);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.SHIPPING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).inProgressAt(existingTs).packedWaitingShipperAt(existingTs)
                .shippingAt(existingTs).deliveredAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.DELIVERED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getShippingAt());
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    void updateOrderStatus_shouldSetCancelledTimestamp_WhenCancelled() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .cancelledAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.CANCELLED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideCancelledAt_WhenAlreadySet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingCancelledAt = LocalDateTime.now().minusHours(1);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .cancelledAt(existingCancelledAt).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.CANCELLED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingCancelledAt, order.getCancelledAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideExistingAssignedAt() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingAssignedAt = LocalDateTime.now().minusDays(1);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .assignedAt(existingAssignedAt).priority("1").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.IN_PROGRESS, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingAssignedAt, order.getAssignedAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideInProgressAt_WhenAlreadySet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(4);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.ASSIGNED)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).inProgressAt(existingTs).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.IN_PROGRESS, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getAssignedAt());
        assertEquals(existingTs, order.getInProgressAt());
    }

    @Test
    void updateOrderStatus_shouldCreateNewNoteWhenOldNotesNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes(null).createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, "new note");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertTrue(response.getNotes().startsWith("[CK kitchenstaff -"));
    }

    @Test
    void updateOrderStatus_shouldCreateNewNoteWhenOldNotesEmptyString() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.ASSIGNED)
                .priority("1").notes("").createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1)).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, "new note");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertTrue(response.getNotes().startsWith("[CK kitchenstaff -"));
        assertFalse(response.getNotes().contains("\n"));
    }

    @Test
    void updateOrderStatus_shouldSetAssignedAt_WhenTransitionToAssigned_AndAssignedAtIsNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertNotNull(order.getAssignedAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideAssignedAt_WhenTransitionToAssigned_AndAlreadySet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(5);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.PENDING)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.ASSIGNED, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getAssignedAt());
    }

    @Test
    void updateOrderStatus_shouldSetPackedTimestamps_WhenAllNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.IN_PROGRESS)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.PACKED_WAITING_SHIPPER, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertNotNull(order.getAssignedAt());
        assertNotNull(order.getInProgressAt());
        assertNotNull(order.getPackedWaitingShipperAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverridePackedTimestamps_WhenAlreadySet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(2);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.IN_PROGRESS)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).inProgressAt(existingTs).packedWaitingShipperAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.PACKED_WAITING_SHIPPER, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getAssignedAt());
        assertEquals(existingTs, order.getInProgressAt());
        assertNotNull(order.getPackedWaitingShipperAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideInProgressAt_WhenTransitionToPacked() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(3);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status(OrderStatus.IN_PROGRESS)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(existingTs).packedWaitingShipperAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.PACKED_WAITING_SHIPPER, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertNotNull(order.getAssignedAt());
        assertEquals(existingTs, order.getInProgressAt());
        assertNotNull(order.getPackedWaitingShipperAt());
    }

    @Test
    void updateOrderStatus_shouldSetAllShippingTimestamps_WhenAllNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.PACKED_WAITING_SHIPPER)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(null).shippingAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertNotNull(order.getAssignedAt());
        assertNotNull(order.getInProgressAt());
        assertNotNull(order.getPackedWaitingShipperAt());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverrideShippingTimestamps_WhenAlreadySet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(2);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.PACKED_WAITING_SHIPPER)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(existingTs).inProgressAt(existingTs).packedWaitingShipperAt(existingTs)
                .shippingAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertEquals(existingTs, order.getAssignedAt());
        assertEquals(existingTs, order.getInProgressAt());
        assertEquals(existingTs, order.getPackedWaitingShipperAt());
        assertNotNull(order.getShippingAt());
    }

    @Test
    void updateOrderStatus_shouldNotOverridePackedAt_WhenTransitionToShipping() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime existingTs = LocalDateTime.now().minusHours(2);
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen)
                .status(OrderStatus.PACKED_WAITING_SHIPPER)
                .priority("1").createdAt(LocalDateTime.now()).requestedDate(LocalDate.now().plusDays(1))
                .assignedAt(null).inProgressAt(null).packedWaitingShipperAt(existingTs).shippingAt(null).build();
        UpdateOrderStatusRequest request = createUpdateStatusRequest(OrderStatus.SHIPPING, null);
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        centralKitchenService.updateOrderStatus("ORD001", request, principal);
        assertNotNull(order.getAssignedAt());
        assertNotNull(order.getInProgressAt());
        assertEquals(existingTs, order.getPackedWaitingShipperAt());
        assertNotNull(order.getShippingAt());
    }

    // ==================== getAllOrders ====================

    @Test
    void getAllOrders_shouldReturnAllOrders_WhenNoFilters() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders(null, null, 0, 20, principal);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldFilterByStatus_WhenStatusProvided() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(orderRepository.findByStatus(eq(OrderStatus.PENDING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("PENDING", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldFilterByStoreId_WhenStoreIdProvided() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findByStore_Id(eq("ST001"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders(null, "ST001", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldFilterByBothStatusAndStoreId() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(storeRepository.existsById("ST001")).thenReturn(true);
        when(orderRepository.findByStore_IdAndStatus(eq("ST001"), eq(OrderStatus.PENDING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("PENDING", "ST001", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldThrowBadRequestException_WhenInvalidStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.getAllOrders("INVALID_STATUS", null, 0, 20, principal));
    }

    @Test
    void getAllOrders_shouldThrowNotFoundException_WhenStoreNotFound() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.existsById("NOT_EXIST")).thenReturn(false);
        assertThrows(NotFoundException.class,
                () -> centralKitchenService.getAllOrders(null, "NOT_EXIST", 0, 20, principal));
    }

    @Test
    void getAllOrders_shouldIgnoreEmptyStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldIgnoreWhitespaceStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PENDING).priority("1").build();
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("   ", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldReturnOrdersWithShippingStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.SHIPPING).priority("1").build();
        when(orderRepository.findByStatus(eq(OrderStatus.SHIPPING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("SHIPPING", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldReturnOrdersWithInProgressStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.IN_PROGRESS).priority("1").build();
        when(orderRepository.findByStatus(eq(OrderStatus.IN_PROGRESS), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("IN_PROGRESS", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllOrders_shouldFilterByProcessingStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.PROCESSING).priority("1").build();
        when(orderRepository.findByStatus(eq(OrderStatus.PROCESSING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("PROCESSING", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        assertEquals(OrderStatus.PROCESSING, result.getContent().get(0).getStatus());
    }

    @Test
    void getAllOrders_shouldFilterByApprovedStatus() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder().id("ORD001").store(store).status(OrderStatus.APPROVED).priority("1").build();
        when(orderRepository.findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = centralKitchenService.getAllOrders("APPROVED", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        assertEquals(OrderStatus.APPROVED, result.getContent().get(0).getStatus());
    }

    // ==================== getProductionPlans ====================

    @Test
    void getProductionPlans_shouldReturnPagedResult() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Product product = Product.builder().id("PROD001").name("Bread").build();
        ProductionPlan plan = ProductionPlan.builder().id("PLN001").product(product).quantity(50).unit("kg")
                .status("PLANNED").startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(7))
                .staff("kitchenstaff").notes("Test plan").build();
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(plan)));

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, principal);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PLN001", result.getContent().get(0).getId());
    }

    @Test
    void getProductionPlans_shouldHandlePlanWithNullProduct() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        ProductionPlan plan = ProductionPlan.builder().id("PLN001").product(null).quantity(50).unit("kg")
                .status("PLANNED").startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(7))
                .staff("kitchenstaff").notes("Test plan").build();
        when(productionPlanRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(plan)));

        Page<ProductionPlanResponse> result = centralKitchenService.getProductionPlans(0, 20, principal);
        assertNull(result.getContent().get(0).getProductId());
        assertNull(result.getContent().get(0).getProductName());
    }

    // ==================== createProductionPlan ====================

    @Test
    void createProductionPlan_shouldSaveSuccessfully() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime startDate = LocalDateTime.now(), endDate = LocalDateTime.now().plusDays(7);
        CreateProductionPlanRequest request = createProductionPlanRequest("PROD001", 100, startDate, endDate, "test plan");
        Product product = Product.builder().id("PROD001").name("Bread").unit("kg").build();
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(productionPlanRepository.count()).thenReturn(0L);
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductionPlanResponse response = centralKitchenService.createProductionPlan(request, principal);
        assertNotNull(response);
        assertTrue(response.getId().startsWith("PLN"));
        assertEquals(100, response.getQuantity());
    }

    @Test
    void createProductionPlan_shouldThrowBadRequestException_WhenEndDateBeforeStartDate() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime startDate = LocalDateTime.now().plusDays(7), endDate = LocalDateTime.now();
        CreateProductionPlanRequest request = createProductionPlanRequest("PROD001", 100, startDate, endDate, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.createProductionPlan(request, principal));
    }

    @Test
    void createProductionPlan_shouldThrowBadRequestException_WhenStartDateEqualsEndDate() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime startDate = LocalDateTime.now();
        CreateProductionPlanRequest request = createProductionPlanRequest("PROD001", 100, startDate, startDate, null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.createProductionPlan(request, principal));
    }

    @Test
    void createProductionPlan_shouldThrowNotFoundException_WhenProductNotFound() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime startDate = LocalDateTime.now(), endDate = LocalDateTime.now().plusDays(7);
        CreateProductionPlanRequest request = createProductionPlanRequest("NOT_EXIST", 100, startDate, endDate, null);
        when(productRepository.findById("NOT_EXIST")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> centralKitchenService.createProductionPlan(request, principal));
    }

    @Test
    void createProductionPlan_shouldGenerateIdWithModuloZero() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        LocalDateTime startDate = LocalDateTime.now(), endDate = LocalDateTime.now().plusDays(7);
        CreateProductionPlanRequest request = createProductionPlanRequest("PROD001", 100, startDate, endDate, "test");
        Product product = Product.builder().id("PROD001").name("Bread").unit("kg").build();
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(productionPlanRepository.count()).thenReturn(999L);
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductionPlanResponse response = centralKitchenService.createProductionPlan(request, principal);
        assertNotNull(response);
        assertTrue(response.getId().matches("PLN\\d{4}000"));
    }

    // ==================== getInventory ====================

    @Test
    void getInventory_shouldReturnPagedInventory() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg")
                .price(BigDecimal.TEN).supplier("A").minStock(10).build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(BigDecimal.valueOf(5)).unit("kg").minStock(10)
                .batchNo("B001").expiryDate(LocalDate.now().plusMonths(6)).supplier("A")
                .updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldReturnNotLowStockWhenQuantityGreaterThanMinStock() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").minStock(10).build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(BigDecimal.valueOf(20)).unit("kg").minStock(10).updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertFalse(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldReturnNotLowStock_WhenMinStockIsNull() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(BigDecimal.valueOf(5)).unit("kg").minStock(null).updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertFalse(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldHandleNullQuantityAndMinStock() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(null).unit("kg").minStock(null).updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertFalse(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldFilterByIngredientId() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        centralKitchenService.getInventory("ING001", null, 0, 20, principal);
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getInventory_shouldFilterByIngredientName() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        centralKitchenService.getInventory(null, "Flour", 0, 20, principal);
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getInventory_shouldFilterByBothIngredientIdAndName() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        centralKitchenService.getInventory("ING001", "Flour", 0, 20, principal);
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getInventory_shouldSkipIngredientIdFilter_WhenIdIsBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        centralKitchenService.getInventory("  ", null, 0, 20, principal);
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getInventory_shouldSkipIngredientNameFilter_WhenNameIsBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        centralKitchenService.getInventory(null, "  ", 0, 20, principal);
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getInventory_shouldReturnLowStock_WhenQuantityEqualsMinStock() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").minStock(10).build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(BigDecimal.valueOf(10)).unit("kg").minStock(10).updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertTrue(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldReturnNotLowStock_WhenQuantityIsNull_AndMinStockSet() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").minStock(5).build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(null).unit("kg").minStock(5).updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);
        assertFalse(result.getContent().get(0).isLowStock());
    }

    @Test
    void getInventory_shouldReturnAllWhenBothFiltersAreBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").unit("kg").build();
        KitchenInventory inv = KitchenInventory.builder().id(1).ingredient(ingredient)
                .quantity(BigDecimal.valueOf(20)).unit("kg").updatedAt(LocalDateTime.now()).build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory("  ", "  ", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        verify(kitchenInventoryRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ==================== getStores ====================

    @Test
    void getStores_shouldReturnFilteredPage() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores("District", "ACTIVE", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        assertEquals("ST001", result.getContent().get(0).getId());
    }

    @Test
    void getStores_shouldFilterByNameOnly() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores("Store", null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStores_shouldFilterByStatusOnly() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores(null, "ACTIVE", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStores_shouldReturnAllWhenNoFilters() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores(null, null, 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStores_shouldSkipNameFilter_WhenNameIsBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores("   ", "ACTIVE", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStores_shouldSkipStatusFilter_WhenStatusIsBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        Page<StoreResponse> result = centralKitchenService.getStores("Store 1", "   ", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStores_shouldReturnAllWhenBothFiltersAreBlank() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));

        Page<StoreResponse> result = centralKitchenService.getStores("  ", "  ", 0, 20, principal);
        assertEquals(1, result.getTotalElements());
        assertEquals("ST001", result.getContent().get(0).getId());
        verify(storeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ==================== getMyKitchen ====================

    @Test
    void getMyKitchen_shouldReturnKitchenResponse_WhenKitchenAssigned() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        KitchenResponse response = centralKitchenService.getMyKitchen(principal);
        assertNotNull(response);
        assertEquals("KIT001", response.getId());
    }

    @Test
    void getMyKitchen_shouldThrowBadRequestException_WhenKitchenNotAssigned() {
        mockCentralKitchenUser("kitchenstaff", null);
        assertThrows(BadRequestException.class, () -> centralKitchenService.getMyKitchen(principal));
    }

    // ==================== getOrderStatuses ====================

    @Test
    void getOrderStatuses_shouldReturnUiStatuses() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        List<String> statuses = centralKitchenService.getOrderStatuses(principal);
        assertTrue(statuses.containsAll(
                List.of("IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING", "DELIVERED", "CANCELLED")));
    }

    // ==================== getOverview ====================

    @Test
    void getOverview_shouldReturnAggregatedMetrics() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class))).thenReturn(10L, 8L, 6L, 4L, 2L, 1L);
        CentralKitchenOverviewResponse response = centralKitchenService.getOverview(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), principal);
        assertNotNull(response);
        assertEquals(10L, response.getPendingUnassignedOrders());
        assertEquals(8L, response.getAssignedToMyKitchen());
    }

    @Test
    void getOverview_shouldWorkWithNullDates() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);
        assertNotNull(centralKitchenService.getOverview(null, null, principal));
    }

    @Test
    void getOverview_shouldWorkWithOnlyFromDate() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);
        assertNotNull(centralKitchenService.getOverview(LocalDate.of(2026, 4, 1), null, principal));
    }

    @Test
    void getOverview_shouldWorkWithOnlyToDate() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);
        assertNotNull(centralKitchenService.getOverview(null, LocalDate.of(2026, 4, 30), principal));
    }

    @Test
    void getOverview_shouldThrowBadRequestException_WhenDateRangeInvalid() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        assertThrows(BadRequestException.class, () ->
                centralKitchenService.getOverview(
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), principal));
    }

    @Test
    void getOverview_shouldThrowBadRequestException_WhenUserHasNoKitchen() {
        mockCentralKitchenUser("kitchenstaff", null);
        assertThrows(BadRequestException.class,
                () -> centralKitchenService.getOverview(null, null, principal));
    }

    @Test
    void getOverview_shouldAcceptSameDateForFromAndTo() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class))).thenReturn(0L);
        LocalDate today = LocalDate.now();
        assertNotNull(centralKitchenService.getOverview(today, today, principal));
    }

    @Test
    void getOverview_shouldReturnAllSixDistinctMetrics() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        when(orderRepository.count(any(Specification.class)))
                .thenReturn(1L, 2L, 3L, 4L, 5L, 6L);
        CentralKitchenOverviewResponse response =
                centralKitchenService.getOverview(
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), principal);
        assertAll(
                () -> assertEquals(1L, response.getPendingUnassignedOrders()),
                () -> assertEquals(2L, response.getAssignedToMyKitchen()),
                () -> assertEquals(3L, response.getInProgressOrders()),
                () -> assertEquals(4L, response.getPackedWaitingShipperOrders()),
                () -> assertEquals(5L, response.getShippingOrders()),
                () -> assertEquals(6L, response.getOverdueOrders())
        );
    }

    // ==================== Authentication / Authorization ====================

    @Test
    void shouldThrowIllegalStateException_WhenPrincipalIsNull() {
        assertThrows(IllegalStateException.class,
                () -> centralKitchenService.getOrderById("ORD001", null));
    }

    @Test
    void shouldThrowNotFoundException_WhenUserNotFound() {
        when(principal.getName()).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> centralKitchenService.getOrderById("ORD001", principal));
    }

    @Test
    void shouldThrowIllegalStateException_WhenUserNotCentralKitchenStaff() {
        when(principal.getName()).thenReturn("staff");
        Role wrongRole = Role.builder().id(2L).name("STORE_STAFF").build();
        user = new User();
        user.setUsername("staff");
        user.setRole(wrongRole);
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));
        assertThrows(IllegalStateException.class,
                () -> centralKitchenService.getOrderById("ORD001", principal));
    }

    @Test
    void shouldThrowIllegalStateException_WhenUserRoleIsNull() {
        when(principal.getName()).thenReturn("staff");
        user = new User();
        user.setUsername("staff");
        user.setRole(null);
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));
        assertThrows(IllegalStateException.class,
                () -> centralKitchenService.getOrderById("ORD001", principal));
    }

    // ==================== FIX LỖI ĐỎ: assignOrder với các status không hợp lệ ====================
    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsProcessing() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.PROCESSING)
                .priority("1")
                .build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void assignOrder_shouldThrowBadRequestException_WhenOrderStatusIsApproved() {
        mockCentralKitchenUser("kitchenstaff", kitchen);
        Order order = Order.builder()
                .id("ORD001")
                .status(OrderStatus.APPROVED)
                .priority("1")
                .build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> centralKitchenService.assignOrder("ORD001", principal));
        verify(orderRepository, never()).save(any());
    }
}