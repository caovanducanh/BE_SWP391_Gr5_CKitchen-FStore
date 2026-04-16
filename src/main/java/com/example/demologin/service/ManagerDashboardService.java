package com.example.demologin.service;

import com.example.demologin.dto.response.KitchenLowStockResponse;
import com.example.demologin.dto.response.ManagerOverviewResponse;
import com.example.demologin.dto.response.StoreLowStockResponse;

import java.util.List;

public interface ManagerDashboardService {
    ManagerOverviewResponse getOverview();

    List<KitchenLowStockResponse> getKitchenLowStockItems();

    List<StoreLowStockResponse> getStoreLowStockItems();
}
