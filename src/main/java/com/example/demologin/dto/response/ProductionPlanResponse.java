package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductionPlanResponse {
    private String id;
    private String orderId;
    private String productId;
    private String productName;
    private String kitchenId;
    private String kitchenName;
    private Integer quantity;
    private String unit;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String staff;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Danh sách nguyên liệu cần dùng (chỉ trả về khi xem chi tiết) */
    private List<PlanIngredientResponse> ingredients;
}
