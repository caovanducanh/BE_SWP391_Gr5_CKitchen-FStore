package com.example.demologin.service;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;

import com.example.demologin.dto.request.admin.KitchenUpsertRequest;
import com.example.demologin.dto.request.admin.OrderPriorityConfigRequest;
import com.example.demologin.dto.request.admin.StoreUpsertRequest;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.dto.response.admin.OrderPriorityConfigResponse;
import com.example.demologin.dto.response.admin.SystemOverviewResponse;

public interface AdminManagementService {
    List<OrderPriorityConfigResponse> getOrderPriorityConfigs();

    OrderPriorityConfigResponse createOrderPriorityConfig(OrderPriorityConfigRequest request);

    OrderPriorityConfigResponse updateOrderPriorityConfig(Integer id, OrderPriorityConfigRequest request);

    Page<StoreResponse> getStores(String name, String status, int page, int size);

    StoreResponse getStoreById(String id);

    StoreResponse createStore(StoreUpsertRequest request);

    StoreResponse updateStore(String id, StoreUpsertRequest request);

    StoreResponse updateStoreStatus(String id, String status);

    Page<KitchenResponse> getKitchens(String name, String status, int page, int size);

    KitchenResponse getKitchenById(String id);

    KitchenResponse createKitchen(KitchenUpsertRequest request);

    KitchenResponse updateKitchen(String id, KitchenUpsertRequest request);

    KitchenResponse updateKitchenStatus(String id, String status);

    SystemOverviewResponse getSystemOverview(LocalDate fromDate, LocalDate toDate);
}
