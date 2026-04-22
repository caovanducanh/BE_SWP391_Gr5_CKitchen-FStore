package com.example.demologin.dto.response.manager;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IngredientFilterOptionResponse {
    private String id;
    private String name;
    private String unit;
}
