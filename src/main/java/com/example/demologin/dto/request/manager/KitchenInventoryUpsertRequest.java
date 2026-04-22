package com.example.demologin.dto.request.manager;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class KitchenInventoryUpsertRequest {

    @NotBlank(message = "kitchenId is required")
    @Size(max = 10, message = "kitchenId must be at most 10 characters")
    private String kitchenId;

    @NotBlank(message = "ingredientId is required")
    @Size(max = 10, message = "ingredientId must be at most 10 characters")
    private String ingredientId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.00", message = "quantity must be greater than or equal to 0")
    private BigDecimal quantity;

    @NotNull(message = "minStock is required")
    @Min(value = 0, message = "minStock must be greater than or equal to 0")
    private Integer minStock;

    @Size(max = 30, message = "batchNo must be at most 30 characters")
    private String batchNo;

    private LocalDate expiryDate;

    @Size(max = 100, message = "supplier must be at most 100 characters")
    private String supplier;
}
