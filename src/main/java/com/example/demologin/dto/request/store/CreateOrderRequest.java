package com.example.demologin.dto.request.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class CreateOrderRequest {

    @NotBlank(message = "storeId is required")
    private String storeId;

    @NotBlank(message = "kitchenId is required")
    private String kitchenId;

    @NotBlank(message = "priority is required")
    private String priority;

    @NotNull(message = "requestedDate is required")
    private LocalDate requestedDate;

    private String notes;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;
}
