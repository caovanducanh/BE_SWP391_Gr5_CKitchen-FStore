package com.example.demologin.service;

import com.example.demologin.dto.request.shipper.ScanPickupQrRequest;
import com.example.demologin.dto.request.shipper.MarkDeliverySuccessRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;

public interface ShipperService {
    Page<OrderResponse> getAvailableOrders(int page, int size, Double lat, Double lon, Principal principal);

    DeliveryResponse scanPickupQr(ScanPickupQrRequest request, Principal principal);

    DeliveryResponse markDeliverySuccess(String deliveryId, MarkDeliverySuccessRequest request, Principal principal);

    Page<DeliveryResponse> getMyDeliveries(int page, int size, Double lat, Double lon, Principal principal);

    OrderHolderResponse getOrderHolder(String orderId, Principal principal);
    
    DeliveryResponse getDeliveryById(String deliveryId, Principal principal);
}