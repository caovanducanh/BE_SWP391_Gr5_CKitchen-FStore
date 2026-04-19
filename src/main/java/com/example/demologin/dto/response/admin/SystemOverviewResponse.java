package com.example.demologin.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class SystemOverviewResponse {
    private long totalUsers;
    private long totalRoles;
    private long totalStores;
    private long activeStores;
    private long totalKitchens;
    private long activeKitchens;
    private long totalProducts;
    private long totalOrders;
    private Map<String, Long> orderStatusCounts;
}
