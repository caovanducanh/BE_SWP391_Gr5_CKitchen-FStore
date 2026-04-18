package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CentralKitchenOverviewResponse {
    private String kitchenId;
    private String kitchenName;
    private long pendingUnassignedOrders;
    private long assignedToMyKitchen;
    private long inProgressOrders;
    private long packedWaitingShipperOrders;
    private long shippingOrders;
    private long overdueOrders;
}
