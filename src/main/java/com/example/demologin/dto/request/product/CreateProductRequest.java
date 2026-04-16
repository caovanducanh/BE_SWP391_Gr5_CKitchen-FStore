package com.example.demologin.dto.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product category is required")
    private String category;

    @NotBlank(message = "Product unit is required")
    private String unit;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Product cost is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product cost must be greater than 0")
    private BigDecimal cost;

    private MultipartFile[] images;
}
