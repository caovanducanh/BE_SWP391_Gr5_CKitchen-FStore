package com.example.demologin.dto.request.centralkitchen;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AssignOrderRequest {

    @NotBlank(message = "kitchenId is required")
    private String kitchenId;
}
