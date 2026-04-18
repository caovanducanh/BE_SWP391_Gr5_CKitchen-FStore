package com.example.demologin.service;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.response.CentralKitchenOverviewResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.KitchenInventoryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.ProductionPlanResponse;
import com.example.demologin.dto.response.StoreResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

public interface CentralKitchenService {
    Page<OrderResponse> getAllOrders(String status, String storeId, int page, int size, Principal principal);

    OrderResponse getOrderById(String orderId, Principal principal);

    OrderResponse assignOrder(String orderId, Principal principal);

    OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request, Principal principal);

    Page<ProductionPlanResponse> getProductionPlans(int page, int size, Principal principal);

    ProductionPlanResponse createProductionPlan(CreateProductionPlanRequest request, Principal principal);

    Page<KitchenInventoryResponse> getInventory(String ingredientId, String ingredientName, int page, int size, Principal principal);

    Page<StoreResponse> getStores(String name, String status, int page, int size, Principal principal);

    KitchenResponse getMyKitchen(Principal principal);

    List<String> getOrderStatuses(Principal principal);

    CentralKitchenOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, Principal principal);
}
