package com.example.demologin.controller;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.OrderTimelineResponse;
import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.dto.response.StoreOverviewResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.service.FranchiseStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseStoreControllerTest {

    @Mock
    private FranchiseStoreService franchiseStoreService;

    @InjectMocks
    private FranchiseStoreController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
    }

    // ==================== createOrder() TESTS ====================

    @Test
    void createOrder_shouldInvokeService() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();

        when(franchiseStoreService.createOrder(request, principal)).thenReturn(response);

        Object result = controller.createOrder(request, principal);

        assertSame(response, result);
        verify(franchiseStoreService).createOrder(request, principal);
    }

    @Test
    void createOrder_whenInvalidRequest_shouldPropagate() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(franchiseStoreService.createOrder(request, principal))
                .thenThrow(new BadRequestException("Invalid order request"));

        assertThrows(BadRequestException.class,
                () -> controller.createOrder(request, principal));
    }

    @Test
    void createOrder_whenUnauthorized_shouldPropagate() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(franchiseStoreService.createOrder(request, principal))
                .thenThrow(new IllegalStateException("Only store staff can access this resource"));

        assertThrows(IllegalStateException.class,
                () -> controller.createOrder(request, principal));
    }

    // ==================== getOrders() TESTS ====================

    @Test
    void getOrders_shouldInvokeServiceWithStatus() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getOrders("PENDING", principal, 0, 20)).thenReturn(page);

        Object result = controller.getOrders("PENDING", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getOrders("PENDING", principal, 0, 20);
    }

    @Test
    void getOrders_shouldInvokeServiceWithNullStatus() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getOrders(null, principal, 0, 20)).thenReturn(page);

        Object result = controller.getOrders(null, 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getOrders(null, principal, 0, 20);
    }

    @Test
    void getOrders_shouldInvokeServiceWithCustomPageParams() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getOrders(null, principal, 5, 50)).thenReturn(page);

        Object result = controller.getOrders(null, 5, 50, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getOrders(null, principal, 5, 50);
    }

    @Test
    void getOrders_whenServiceThrowsException_shouldPropagate() {
        when(franchiseStoreService.getOrders("INVALID", principal, 0, 20))
                .thenThrow(new BadRequestException("Invalid order status"));

        assertThrows(BadRequestException.class,
                () -> controller.getOrders("INVALID", 0, 20, principal));
    }

    // ==================== getOrderById() TESTS ====================

    @Test
    void getOrderById_shouldInvokeService() {
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(franchiseStoreService.getOrderById("ORD001")).thenReturn(response);

        Object result = controller.getOrderById("ORD001");

        assertSame(response, result);
        verify(franchiseStoreService).getOrderById("ORD001");
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldPropagate() {
        when(franchiseStoreService.getOrderById("ORD999"))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderById("ORD999"));
    }

    // ==================== getOrderTimeline() TESTS ====================

    @Test
    void getOrderTimeline_shouldInvokeService() {
        OrderTimelineResponse response = OrderTimelineResponse.builder().orderId("ORD001").build();
        when(franchiseStoreService.getOrderTimeline("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderTimeline("ORD001", principal);

        assertSame(response, result);
        verify(franchiseStoreService).getOrderTimeline("ORD001", principal);
    }

    @Test
    void getOrderTimeline_whenOrderNotFound_shouldPropagate() {
        when(franchiseStoreService.getOrderTimeline("ORD999", principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderTimeline("ORD999", principal));
    }

    // ==================== getOrderStatuses() TESTS ====================

    @Test
    void getOrderStatuses_shouldInvokeService() {
        List<String> statuses = List.of("PENDING", "ASSIGNED", "IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING", "DELIVERED", "CANCELLED");
        when(franchiseStoreService.getOrderStatuses()).thenReturn(statuses);

        Object result = controller.getOrderStatuses();

        assertSame(statuses, result);
        verify(franchiseStoreService).getOrderStatuses();
    }

    // ==================== getDeliveries() TESTS ====================

    @Test
    void getDeliveries_shouldInvokeServiceWithStatus() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getDeliveries("SHIPPING", principal, 0, 20)).thenReturn(page);

        Object result = controller.getDeliveries("SHIPPING", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getDeliveries("SHIPPING", principal, 0, 20);
    }

    @Test
    void getDeliveries_shouldInvokeServiceWithNullStatus() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getDeliveries(null, principal, 0, 20)).thenReturn(page);

        Object result = controller.getDeliveries(null, 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getDeliveries(null, principal, 0, 20);
    }

    @Test
    void getDeliveries_shouldInvokeServiceWithCustomPageParams() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getDeliveries(null, principal, 5, 50)).thenReturn(page);

        Object result = controller.getDeliveries(null, 5, 50, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getDeliveries(null, principal, 5, 50);
    }

    @Test
    void getDeliveries_whenInvalidStatus_shouldPropagate() {
        when(franchiseStoreService.getDeliveries("INVALID", principal, 0, 20))
                .thenThrow(new BadRequestException("Invalid delivery status"));

        assertThrows(BadRequestException.class,
                () -> controller.getDeliveries("INVALID", 0, 20, principal));
    }

    // ==================== confirmReceipt() TESTS ====================

    @Test
    void confirmReceipt_shouldInvokeService() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(franchiseStoreService.confirmReceipt("DEL001", request)).thenReturn(response);

        Object result = controller.confirmReceipt("DEL001", request);

        assertSame(response, result);
        verify(franchiseStoreService).confirmReceipt("DEL001", request);
    }

    @Test
    void confirmReceipt_whenDeliveryNotFound_shouldPropagate() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(franchiseStoreService.confirmReceipt("DEL999", request))
                .thenThrow(new NotFoundException("Delivery not found: DEL999"));

        assertThrows(NotFoundException.class,
                () -> controller.confirmReceipt("DEL999", request));
    }

    @Test
    void confirmReceipt_whenInvalidRequest_shouldPropagate() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(franchiseStoreService.confirmReceipt("DEL001", request))
                .thenThrow(new BadRequestException("temperatureOk is required"));

        assertThrows(BadRequestException.class,
                () -> controller.confirmReceipt("DEL001", request));
    }

    // ==================== confirmReceiptByOrder() TESTS ====================

    @Test
    void confirmReceiptByOrder_shouldInvokeService() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal)).thenReturn(response);

        Object result = controller.confirmReceiptByOrder("ORD001", request, principal);

        assertSame(response, result);
        verify(franchiseStoreService).confirmReceiptByOrderId("ORD001", request, principal);
    }

    @Test
    void confirmReceiptByOrder_whenOrderNotFound_shouldPropagate() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(franchiseStoreService.confirmReceiptByOrderId("ORD999", request, principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.confirmReceiptByOrder("ORD999", request, principal));
    }

    @Test
    void confirmReceiptByOrder_whenDeliveryNotFound_shouldPropagate() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal))
                .thenThrow(new NotFoundException("Delivery not found for order: ORD001"));

        assertThrows(NotFoundException.class,
                () -> controller.confirmReceiptByOrder("ORD001", request, principal));
    }

    @Test
    void confirmReceiptByOrder_whenInvalidRequest_shouldPropagate() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        when(franchiseStoreService.confirmReceiptByOrderId("ORD001", request, principal))
                .thenThrow(new BadRequestException("temperatureOk is required"));

        assertThrows(BadRequestException.class,
                () -> controller.confirmReceiptByOrder("ORD001", request, principal));
    }

    // ==================== getStoreInventory() TESTS ====================

    @Test
    void getStoreInventory_shouldInvokeServiceWithNullParams() {
        Page<StoreInventoryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getStoreInventory(null, null, principal, 0, 20)).thenReturn(page);

        Object result = controller.getStoreInventory(null, null, 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getStoreInventory(null, null, principal, 0, 20);
    }

    @Test
    void getStoreInventory_shouldInvokeServiceWithSearchParams() {
        Page<StoreInventoryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getStoreInventory("PROD001", "Bánh", principal, 0, 20)).thenReturn(page);

        Object result = controller.getStoreInventory("PROD001", "Bánh", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getStoreInventory("PROD001", "Bánh", principal, 0, 20);
    }

    @Test
    void getStoreInventory_shouldInvokeServiceWithCustomPageParams() {
        Page<StoreInventoryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getStoreInventory(null, null, principal, 5, 50)).thenReturn(page);

        Object result = controller.getStoreInventory(null, null, 5, 50, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getStoreInventory(null, null, principal, 5, 50);
    }

    @Test
    void getStoreInventory_whenServiceThrowsException_shouldPropagate() {
        when(franchiseStoreService.getStoreInventory(any(), any(), any(Principal.class), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class,
                () -> controller.getStoreInventory(null, null, 0, 20, principal));
    }

    // ==================== getMyStore() TESTS ====================

    @Test
    void getMyStore_shouldInvokeService() {
        StoreResponse response = StoreResponse.builder().id("ST001").name("Store 1").build();
        when(franchiseStoreService.getMyStore(principal)).thenReturn(response);

        Object result = controller.getMyStore(principal);

        assertNotNull(result);
        assertSame(response, result);
        verify(franchiseStoreService).getMyStore(principal);
    }

    @Test
    void getMyStore_whenStoreNotFound_shouldPropagate() {
        when(franchiseStoreService.getMyStore(principal))
                .thenThrow(new NotFoundException("Store not found for user"));

        assertThrows(NotFoundException.class,
                () -> controller.getMyStore(principal));
    }

    @Test
    void getMyStore_whenUnauthorized_shouldPropagate() {
        when(franchiseStoreService.getMyStore(principal))
                .thenThrow(new IllegalStateException("Only store staff can access this resource"));

        assertThrows(IllegalStateException.class,
                () -> controller.getMyStore(principal));
    }

    // ==================== getOverview() TESTS ====================

    @Test
    void getOverview_shouldInvokeService() {
        StoreOverviewResponse response = StoreOverviewResponse.builder().storeId("ST001").build();
        when(franchiseStoreService.getOverview(principal)).thenReturn(response);

        Object result = controller.getOverview(principal);

        assertSame(response, result);
        verify(franchiseStoreService).getOverview(principal);
    }

    @Test
    void getOverview_whenServiceThrowsException_shouldPropagate() {
        when(franchiseStoreService.getOverview(principal))
                .thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class,
                () -> controller.getOverview(principal));
    }

    // ==================== getAvailableProducts() TESTS ====================

    @Test
    void getAvailableProducts_shouldInvokeServiceWithNullParams() {
        Page<ProductResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getAvailableProducts(null, null, 0, 20)).thenReturn(page);

        Object result = controller.getAvailableProducts(null, null, 0, 20);

        assertSame(page, result);
        verify(franchiseStoreService).getAvailableProducts(null, null, 0, 20);
    }

    @Test
    void getAvailableProducts_shouldInvokeServiceWithSearchParams() {
        Page<ProductResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getAvailableProducts("Bánh", "BAKERY", 0, 20)).thenReturn(page);

        Object result = controller.getAvailableProducts("Bánh", "BAKERY", 0, 20);

        assertSame(page, result);
        verify(franchiseStoreService).getAvailableProducts("Bánh", "BAKERY", 0, 20);
    }

    @Test
    void getAvailableProducts_shouldInvokeServiceWithCustomPageParams() {
        Page<ProductResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getAvailableProducts(null, null, 5, 50)).thenReturn(page);

        Object result = controller.getAvailableProducts(null, null, 5, 50);

        assertSame(page, result);
        verify(franchiseStoreService).getAvailableProducts(null, null, 5, 50);
    }

    @Test
    void getAvailableProducts_shouldInvokeServiceWithDefaultPageParams() {
        Page<ProductResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getAvailableProducts(null, null, 0, 20)).thenReturn(page);

        Object result = controller.getAvailableProducts(null, null, 0, 20);

        assertSame(page, result);
        verify(franchiseStoreService).getAvailableProducts(null, null, 0, 20);
    }

    @Test
    void getAvailableProducts_whenServiceThrowsException_shouldPropagate() {
        when(franchiseStoreService.getAvailableProducts(any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class,
                () -> controller.getAvailableProducts(null, null, 0, 20));
    }
    // ==================== BỔ SUNG COVERAGE ====================

    @Test
    void getOrders_shouldHandleEmptyStatus() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getOrders("", principal, 0, 20)).thenReturn(page);

        Object result = controller.getOrders("", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getOrders("", principal, 0, 20);
    }

    @Test
    void getDeliveries_shouldHandleEmptyStatus() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getDeliveries("", principal, 0, 20)).thenReturn(page);

        Object result = controller.getDeliveries("", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getDeliveries("", principal, 0, 20);
    }

    @Test
    void getStoreInventory_shouldHandleEmptyProductIdAndName() {
        Page<StoreInventoryResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getStoreInventory("", "", principal, 0, 20)).thenReturn(page);

        Object result = controller.getStoreInventory("", "", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getStoreInventory("", "", principal, 0, 20);
    }

    @Test
    void getAvailableProducts_shouldHandleEmptyNameAndCategory() {
        Page<ProductResponse> page = new PageImpl<>(List.of());
        when(franchiseStoreService.getAvailableProducts("", "", 0, 20)).thenReturn(page);

        Object result = controller.getAvailableProducts("", "", 0, 20);

        assertSame(page, result);
        verify(franchiseStoreService).getAvailableProducts("", "", 0, 20);
    }

    @Test
    void getOrderStatuses_shouldReturnList() {
        List<String> statuses = List.of("PENDING", "ASSIGNED", "IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING", "DELIVERED", "CANCELLED");
        when(franchiseStoreService.getOrderStatuses()).thenReturn(statuses);

        Object result = controller.getOrderStatuses();

        assertSame(statuses, result);
    }
}
