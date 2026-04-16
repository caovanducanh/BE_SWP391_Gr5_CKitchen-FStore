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

    @InjectMocks
    private FranchiseStoreServiceImpl franchiseStoreService;

    private Store store;
    private Kitchen kitchen;
    private Product product;
    private Principal principal;

    @BeforeEach
    void setUp() {
        store = Store.builder().id("ST001").name("Store 1").build();
        kitchen = Kitchen.builder().id("KIT001").name("Kitchen 1").build();
        product = Product.builder().id("PROD001").name("Product 1").build();
        principal = mock(Principal.class);
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        
        OrderItemRequest itemRequest = mock(OrderItemRequest.class);
        when(itemRequest.getProductId()).thenReturn("PROD001");
        when(itemRequest.getQuantity()).thenReturn(10);
        when(itemRequest.getUnit()).thenReturn("piece");

        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getStoreId()).thenReturn("ST001");
        when(request.getKitchenId()).thenReturn("KIT001");
        when(request.getPriority()).thenReturn("HIGH");
        when(request.getRequestedDate()).thenReturn(LocalDate.now().plusDays(1));
        when(request.getNotes()).thenReturn("Notes");
        when(request.getItems()).thenReturn(List.of(itemRequest));

        when(storeRepository.findById("ST001")).thenReturn(Optional.of(store));
        when(kitchenRepository.findById("KIT001")).thenReturn(Optional.of(kitchen));
        when(productRepository.findById("PROD001")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.count()).thenReturn(100L);

        // Act
        OrderResponse response = franchiseStoreService.createOrder(request, principal);

        // Assert
        assertNotNull(response);
        assertEquals("ST001", response.getStoreId());
        assertEquals("KIT001", response.getKitchenId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("HIGH", response.getPriority());
        assertEquals(1, response.getItems().size());
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    void createOrder_shouldThrowNotFoundExceptionWhenStoreNotFound() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getStoreId()).thenReturn("ST404");

        when(storeRepository.findById("ST404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> franchiseStoreService.createOrder(request, principal));
    }

    @Test
    void getOrders_shouldReturnPagedOrders() {
        // Arrange
        Order order = Order.builder().id("ORD001").store(store).kitchen(kitchen).status("PENDING").build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrder_Id("ORD001")).thenReturn(Collections.emptyList());

        // Act
        Page<OrderResponse> result = franchiseStoreService.getOrders(null, null, 0, 10);

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
        StoreInventory inventory = StoreInventory.builder()
                .id(1)
                .store(store)
                .product(product)
                .quantity(5)
                .minStock(10)
                .build();
        Page<StoreInventory> page = new PageImpl<>(List.of(inventory));
        
        when(storeRepository.findById("ST001")).thenReturn(Optional.of(store));
        when(storeInventoryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        Page<StoreInventoryResponse> result = franchiseStoreService.getStoreInventory("ST001", 0, 10);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLowStock());
        verify(storeInventoryRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }
}
