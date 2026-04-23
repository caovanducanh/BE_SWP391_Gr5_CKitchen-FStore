package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCheckResponse {
    private String productId;
    private String productName;
    private Integer requestedQuantity;
    private List<IngredientCheckDetail> ingredients;
    private boolean canProduce;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientCheckDetail {
        private String ingredientId;
        private String ingredientName;
        private BigDecimal requiredQuantity;
        private BigDecimal availableQuantity;
        private String unit;
        private boolean isSufficient;
        private BigDecimal shortage;
    }
}
