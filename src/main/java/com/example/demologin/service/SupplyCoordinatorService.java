package com.example.demologin.service;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderPickupQrResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

public interface SupplyCoordinatorService {
    Page<KitchenResponse> getKitchens(int page, int size, Principal principal);

    Page<OrderResponse> getOrders(String status,
                                  String priority,
                                  String storeId,
                                  String kitchenId,
                                  LocalDate fromDate,
                                  LocalDate toDate,
                                  int page,
                                  int size,
                                  Principal principal);

    OrderResponse getOrderById(String orderId, Principal principal);

    OrderResponse assignOrderToKitchen(String orderId, AssignOrderKitchenRequest request, Principal principal);

    SupplyCoordinatorOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, Principal principal);

    DeliveryResponse scheduleDelivery(ScheduleDeliveryRequest request, Principal principal);

    Page<DeliveryResponse> getDeliveries(String status, int page, int size, Principal principal);

    DeliveryResponse updateDeliveryStatus(String deliveryId, UpdateDeliveryStatusRequest request, Principal principal);

    OrderPickupQrResponse getOrderPickupQr(String orderId, Principal principal);

    OrderHolderResponse getOrderHolder(String orderId, Principal principal);

    OrderResponse handleIssue(String orderId, HandleIssueRequest request, Principal principal);

    List<String> getOrderStatuses(Principal principal);

    List<String> getDeliveryStatuses(Principal principal);
}
