package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.request.store.OrderItemRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.entity.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

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
        assertEquals("PENDING", response.getStatus());
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
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status("PENDING").build();
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
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status("PENDING").build();
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));

        OrderResponse response = franchiseStoreService.getOrderById("ORD001");

        assertEquals("ORD001", response.getId());
    }

    @Test
    void getDeliveryByOrderId_shouldReturnDelivery() {
        Order order = Order.builder().id("ORD001").build();
        Delivery delivery = Delivery.builder().id("DEL001").order(order).status("SHIPPING").build();
        
        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder_Id("ORD001")).thenReturn(Optional.of(delivery));

        DeliveryResponse response = franchiseStoreService.getDeliveryByOrderId("ORD001");

        assertEquals("DEL001", response.getId());
        assertEquals("SHIPPING", response.getStatus());
    }

    @Test
    void confirmReceipt_shouldUpdateDeliveryAndOrderStatus() {
        // Arrange
        Order order = Order.builder().id("ORD001").status("SHIPPING").build();
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
        assertEquals("DELIVERED", order.getStatus());
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
}
