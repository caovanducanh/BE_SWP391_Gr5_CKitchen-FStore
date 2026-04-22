package com.example.demologin.service;

import com.example.demologin.dto.request.centralkitchen.ImportIngredientBatchRequest;
import com.example.demologin.dto.response.IngredientBatchResponse;
import com.example.demologin.dto.response.KitchenInventoryDetailResponse;
import org.springframework.data.domain.Page;

import java.security.Principal;

public interface IngredientBatchService {

    /**
     * Kitchen staff nhập lô nguyên liệu mới vào kho bếp.
     * Tự động tạo/cập nhật KitchenInventory (totalQuantity).
     */
    IngredientBatchResponse importBatch(ImportIngredientBatchRequest request, Principal principal);

    /**
     * Danh sách lô nguyên liệu trong bếp, hỗ trợ filter theo ingredientId, status.
     */
    Page<IngredientBatchResponse> getBatches(
            String ingredientId, String ingredientName, String status,
            int page, int size, Principal principal);

    /**
     * Chi tiết 1 lô nguyên liệu.
     */
    IngredientBatchResponse getBatchById(String id, Principal principal);

    /**
     * Tồn kho tổng hợp (grouped by ingredient) + danh sách lô chi tiết.
     */
    Page<KitchenInventoryDetailResponse> getInventory(
            String ingredientId, String ingredientName,
            Boolean lowStock, int page, int size, Principal principal);
}
