package com.example.demologin.service;

import com.example.demologin.dto.request.manager.KitchenInventoryUpsertRequest;

import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.manager.IngredientFilterOptionResponse;
import com.example.demologin.dto.response.manager.ManagerKitchenInventoryGroupResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ManagerInventoryService {
    Page<ManagerKitchenInventoryGroupResponse> getKitchenInventory(
            String kitchenId,
            Boolean lowStock,
            int page,
            int size
    );

    List<KitchenResponse> getAllKitchens();

    List<IngredientFilterOptionResponse> getAllIngredientsForFilter();

    List<String> getAllSuppliersForFilter();
}
