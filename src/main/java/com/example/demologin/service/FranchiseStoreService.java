package com.example.demologin.service;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;

public interface FranchiseStoreService {
    OrderResponse createOrder(CreateOrderRequest request, Principal principal);
    Page<OrderResponse> getOrders(String storeId, String status, int page, int size);
    OrderResponse getOrderById(String orderId);
    DeliveryResponse getDeliveryByOrderId(String orderId);
    DeliveryResponse confirmReceipt(String deliveryId, ConfirmReceiptRequest request);
    Page<StoreInventoryResponse> getStoreInventory(String storeId, int page, int size);
}
