package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RecipeResponse {
    private Integer id;
    private String productId;
    private String productName;
    private String ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private String unit;
    private LocalDateTime createdAt;
}
