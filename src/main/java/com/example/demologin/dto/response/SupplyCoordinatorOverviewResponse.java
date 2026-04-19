package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupplyCoordinatorOverviewResponse {
    private long totalOrders;
    private long pendingOrders;
    private long assignedOrders;
    private long inProgressOrders;
    private long packedWaitingShipperOrders;
    private long shippingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long overdueOrders;
    private long unassignedOrders;
    private long activeDeliveries;
}
