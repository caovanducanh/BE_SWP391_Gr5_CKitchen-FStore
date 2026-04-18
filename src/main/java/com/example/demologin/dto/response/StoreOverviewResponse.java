package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreOverviewResponse {
    private String storeId;
    private String storeName;
    private long totalOrders;
    private long pendingOrders;
    private long inProgressOrders;
    private long shippingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long lowStockItems;
    private long activeDeliveries;
}
