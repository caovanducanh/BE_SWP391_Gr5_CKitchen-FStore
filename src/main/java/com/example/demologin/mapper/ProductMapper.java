package com.example.demologin.mapper;

import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .unit(product.getUnit())
                .price(product.getPrice())
                .cost(product.getCost())
                .imageUrl(product.getImageUrl() == null ? new ArrayList<>() : product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}