package com.example.demologin.service;

import com.example.demologin.dto.response.ProductResponse;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.OrderTimelineResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.dto.response.StoreResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;

public interface FranchiseStoreService {
    OrderResponse createOrder(CreateOrderRequest request, Principal principal);
    Page<OrderResponse> getOrders(String status, Principal principal, int page, int size);
    OrderResponse getOrderById(String orderId);
    OrderTimelineResponse getOrderTimeline(String orderId, Principal principal);
    DeliveryResponse getDeliveryByOrderId(String orderId);
    DeliveryResponse confirmReceipt(String deliveryId, ConfirmReceiptRequest request);
    Page<StoreInventoryResponse> getStoreInventory(String productId, String productName, Principal principal, int page, int size);
    StoreResponse getMyStore(Principal principal);
    Page<ProductResponse> getAvailableProducts(String name, String category, int page, int size);
}
