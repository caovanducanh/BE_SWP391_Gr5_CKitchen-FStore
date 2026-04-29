package com.example.demologin.dto.response;

import com.example.demologin.enums.ProductCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private ProductCategory category;
    private String unit;
    private BigDecimal price;
    private BigDecimal cost;
    private List<String> imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
