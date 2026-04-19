package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Order;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        store = Store.builder().id("ST001").name("Store 1").build();
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();
    }

    private User mockCurrentSupplyCoordinator() {
        when(principal.getName()).thenReturn("supply");

        Role role = Role.builder().name("SUPPLY_COORDINATOR").build();
        User user = new User();
        user.setUserId(10L);
        user.setUsername("supply");
        user.setFullName("Supply User");
        user.setRole(role);

        when(userRepository.findByUsername("supply")).thenReturn(Optional.of(user));
        return user;
    }

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
    void scheduleDelivery_shouldCreateShippingDeliveryAndUpdateOrder() {
        User coordinator = mockCurrentSupplyCoordinator();

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
        when(deliveryRepository.count()).thenReturn(12L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = service.scheduleDelivery(request, principal);

        assertEquals("SHIPPING", response.getStatus());
        assertEquals("ORD001", response.getOrderId());
        assertEquals("Supply User", response.getCoordinatorName());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertNotNull(order.getShippingAt());
        assertEquals(coordinator.getFullName(), response.getCoordinatorName());
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
    void getDeliveries_shouldThrowWhenStatusInvalid() {
        mockCurrentSupplyCoordinator();

        assertThrows(BadRequestException.class, () -> service.getDeliveries("WRONG", 0, 20, principal));
    }

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

        User coordinator = new User();
        coordinator.setFullName("Supply User");

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
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of());

        OrderResponse response = service.handleIssue("ORD001", request, principal);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("CANCELLED", delivery.getStatus());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void getOverview_shouldReturnAggregatedValues() {
        User coordinator = mockCurrentSupplyCoordinator();

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
    void getOrderStatuses_shouldRequireSupplyCoordinator() {
        when(principal.getName()).thenReturn("manager");
        User user = new User();
        user.setUsername("manager");
        user.setRole(Role.builder().name("MANAGER").build());
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> service.getOrderStatuses(principal));
        verify(orderRepository, never()).findById(anyString());
    }

    @Test
    void getDeliveries_shouldReturnCoordinatorScopedPage() {
        mockCurrentSupplyCoordinator();

        Order order = Order.builder().id("ORD001").status(OrderStatus.SHIPPING).store(store).build();
        User coordinator = new User();
        coordinator.setFullName("Supply User");
        Delivery delivery = Delivery.builder()
                .id("DEL001")
                .order(order)
                .coordinator(coordinator)
                .status("SHIPPING")
                .assignedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findByCoordinator_UserIdAndStatus(anyLong(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        Page<DeliveryResponse> response = service.getDeliveries("SHIPPING", 0, 20, principal);

        assertEquals(1, response.getTotalElements());
        assertEquals("DEL001", response.getContent().get(0).getId());
        assertEquals("SHIPPING", response.getContent().get(0).getStatus());
    }
}
