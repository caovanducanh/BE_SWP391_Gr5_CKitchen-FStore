package com.example.demologin.service;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.*;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

public interface CentralKitchenService {
    Page<OrderResponse> getAllOrders(String status, String storeId, int page, int size, Principal principal);
    OrderResponse getOrderById(String orderId, Principal principal);
    OrderResponse assignOrder(String orderId, Principal principal);
    OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request, Principal principal);

    // Production Plan
    Page<ProductionPlanResponse> getProductionPlans(int page, int size, Principal principal);
    ProductionPlanResponse getProductionPlanById(String planId, Principal principal);
    ProductionPlanResponse createProductionPlan(CreateProductionPlanRequest request, Principal principal);
    ProductionPlanResponse startProductionPlan(String planId, Principal principal);
    ProductionPlanResponse completeProductionPlan(String planId, String notes, LocalDate expiryDate, Principal principal);
    ProductionPlanResponse cancelProductionPlan(String planId, String notes, Principal principal);

    // Product Batches (thành phẩm)
    Page<BatchResponse> getProductBatches(String productId, String status, int page, int size, Principal principal);
    BatchResponse getProductBatchById(String batchId, Principal principal);
    BatchResponse updateBatch(String batchId, com.example.demologin.dto.request.centralkitchen.UpdateBatchRequest request, Principal principal);

    // Inventory (dùng IngredientBatchService)
    Page<KitchenInventoryDetailResponse> getInventory(String ingredientId, String ingredientName, Boolean lowStock, int page, int size, Principal principal);
    Page<ProductResponse> getProducts(String search, String category, int page, int size, Principal principal);
    Page<KitchenProductInventoryResponse> getProductInventory(String productId, String productName, int page, int size, Principal principal);

    Page<StoreResponse> getStores(String name, String status, int page, int size, Principal principal);
    KitchenResponse getMyKitchen(Principal principal);
    List<String> getOrderStatuses(Principal principal);
    CentralKitchenOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, Principal principal);
    RecipeCheckResponse checkRecipeAvailability(String productId, Integer quantity, Principal principal);
}
