package com.example.demologin.service;

import com.example.demologin.dto.response.ProductResponse;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.OrderTimelineResponse;
import com.example.demologin.dto.response.StoreOverviewResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.dto.response.StoreResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.util.List;

public interface FranchiseStoreService {
    OrderResponse createOrder(CreateOrderRequest request, Principal principal);
    Page<OrderResponse> getOrders(String status, Principal principal, int page, int size);
    OrderResponse getOrderById(String orderId);
    OrderTimelineResponse getOrderTimeline(String orderId, Principal principal);
    Page<DeliveryResponse> getDeliveries(String status, Principal principal, int page, int size);
    DeliveryResponse confirmReceipt(String deliveryId, ConfirmReceiptRequest request);
    Page<StoreInventoryResponse> getStoreInventory(String productId, String productName, Principal principal, int page, int size);
    StoreResponse getMyStore(Principal principal);
    StoreOverviewResponse getOverview(Principal principal);
    List<String> getOrderStatuses();
    Page<ProductResponse> getAvailableProducts(String name, String category, int page, int size);
}
