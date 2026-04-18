package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.CentralKitchenOverviewResponse;
import com.example.demologin.dto.response.KitchenInventoryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Role;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralKitchenServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ProductionPlanRepository productionPlanRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private KitchenInventoryRepository kitchenInventoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CentralKitchenServiceImpl centralKitchenService;

    private Principal principal;
    private Kitchen kitchen;
    private Store store;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        kitchen = Kitchen.builder().id("KIT001").name("Central Kitchen").build();
        store = Store.builder().id("ST001").name("Store 1").status("ACTIVE").build();
    }

    private void mockCentralKitchenUser(String username, Kitchen assignedKitchen) {
        when(principal.getName()).thenReturn(username);
        Role role = Role.builder().name("CENTRAL_KITCHEN_STAFF").build();
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setKitchen(assignedKitchen);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    @Test
    void getOrderById_shouldReturnOrderResponse() {
        mockCentralKitchenUser("kitchen", kitchen);
        Product product = Product.builder().id("PROD001").name("Bread").build();
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .kitchen(kitchen)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1))
                .build();
        OrderItem item = OrderItem.builder().id(1).order(order).product(product).quantity(5).unit("piece").createdAt(LocalDateTime.now()).build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(List.of(item));

        OrderResponse response = centralKitchenService.getOrderById("ORD001", principal);

        assertEquals("ORD001", response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void assignOrder_shouldAssignKitchenAndSetAssignedAt() {
        mockCentralKitchenUser("kitchen", kitchen);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1))
                .build();

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.assignOrder("ORD001", principal);

        assertEquals(OrderStatus.ASSIGNED, response.getStatus());
        assertEquals("KIT001", response.getKitchenId());
        assertNotNull(order.getAssignedAt());
    }

    @Test
    void updateOrderStatus_shouldSetTimelineAndAppendNote() {
        mockCentralKitchenUser("kitchen", kitchen);
        Order order = Order.builder()
                .id("ORD001")
                .store(store)
                .kitchen(kitchen)
                .status(OrderStatus.ASSIGNED)
                .notes("old-note")
                .createdAt(LocalDateTime.now())
                .requestedDate(LocalDate.now().plusDays(1))
                .build();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        ReflectionTestUtils.setField(request, "status", OrderStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(request, "notes", "started production");

        when(principal.getName()).thenReturn("kitchen");
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        OrderResponse response = centralKitchenService.updateOrderStatus("ORD001", request, principal);

        assertEquals(OrderStatus.IN_PROGRESS, response.getStatus());
        assertNotNull(order.getInProgressAt());
        assertTrue(response.getNotes().contains("started production"));
    }

    @Test
    void getAllOrders_shouldThrowWhenStatusInvalid() {
        mockCentralKitchenUser("kitchen", kitchen);

        assertThrows(BadRequestException.class,
                () -> centralKitchenService.getAllOrders("WRONG_STATUS", null, 0, 20, principal));
    }

    @Test
    void getInventory_shouldReturnPagedInventory() {
        mockCentralKitchenUser("kitchen", kitchen);
        Ingredient ingredient = Ingredient.builder().id("ING001").name("Flour").build();
        KitchenInventory inv = KitchenInventory.builder()
            .id(1)
            .ingredient(ingredient)
            .unit("kg")
            .minStock(10)
            .quantity(java.math.BigDecimal.valueOf(5))
            .build();
        when(kitchenInventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<KitchenInventoryResponse> result = centralKitchenService.getInventory(null, null, 0, 20, principal);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLowStock());
    }

    @Test
    void getStores_shouldReturnFilteredPage() {
        mockCentralKitchenUser("kitchen", kitchen);
        when(storeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));

        Page<StoreResponse> result = centralKitchenService.getStores("District", "ACTIVE", 0, 20, principal);

        assertEquals(1, result.getTotalElements());
        assertEquals("ST001", result.getContent().get(0).getId());

        verify(storeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getMyKitchen_shouldThrowWhenKitchenNotAssigned() {
        mockCentralKitchenUser("kitchen", null);

        assertThrows(BadRequestException.class, () -> centralKitchenService.getMyKitchen(principal));
    }

    @Test
    void getOrderStatuses_shouldReturnUiStatuses() {
        mockCentralKitchenUser("kitchen", kitchen);

        List<String> statuses = centralKitchenService.getOrderStatuses(principal);

        assertTrue(statuses.contains("IN_PROGRESS"));
        assertTrue(statuses.contains("PACKED_WAITING_SHIPPER"));
        assertTrue(statuses.contains("SHIPPING"));
        assertTrue(statuses.contains("DELIVERED"));
    }

    @Test
    void getOverview_shouldThrowWhenDateRangeInvalid() {
        mockCentralKitchenUser("kitchen", kitchen);

        assertThrows(BadRequestException.class,
                () -> centralKitchenService.getOverview(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), principal));
    }

    @Test
    void getOverview_shouldReturnAggregatedMetrics() {
        mockCentralKitchenUser("kitchen", kitchen);

        when(orderRepository.count(any(Specification.class)))
                .thenReturn(10L, 8L, 6L, 4L, 2L, 1L);

        CentralKitchenOverviewResponse response = centralKitchenService.getOverview(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), principal
        );

        assertEquals("KIT001", response.getKitchenId());
        assertEquals(10L, response.getPendingUnassignedOrders());
        assertEquals(8L, response.getAssignedToMyKitchen());
        assertEquals(6L, response.getInProgressOrders());
        assertEquals(4L, response.getPackedWaitingShipperOrders());
        assertEquals(2L, response.getShippingOrders());
        assertEquals(1L, response.getOverdueOrders());
    }
}
