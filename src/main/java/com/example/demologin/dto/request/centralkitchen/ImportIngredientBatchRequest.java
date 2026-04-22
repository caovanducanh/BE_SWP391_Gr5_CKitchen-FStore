package com.example.demologin.dto.request.centralkitchen;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ImportIngredientBatchRequest {

    @NotBlank(message = "ingredientId is required")
    private String ingredientId;

    @NotBlank(message = "batchNo is required")
    @Size(max = 30, message = "batchNo must be at most 30 characters")
    private String batchNo;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "expiryDate is required")
    private LocalDate expiryDate;

    @Size(max = 100, message = "supplier must be at most 100 characters")
    private String supplier;

    @DecimalMin(value = "0", message = "importPrice must be >= 0")
    private BigDecimal importPrice;

    private LocalDate importDate;

    @Size(max = 500, message = "notes must be at most 500 characters")
    private String notes;

    /** Mức tồn kho tối thiểu cho nguyên liệu này trong bếp (tuỳ chọn, chỉ set lần đầu) */
    @Min(value = 0, message = "minStock must be >= 0")
    private Integer minStock;
}
