package com.example.demologin.dto.request.centralkitchen;

import com.example.demologin.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateOrderStatusRequest {

    @NotNull(message = "status is required")
    @Schema(
            description = "Order status",
            allowableValues = {
                    "IN_PROGRESS",
                    "PACKED_WAITING_SHIPPER",
                    "SHIPPING",
                    "DELIVERED",
                    "CANCELLED"
            }
    )
    private OrderStatus status;

    @Schema(description = "Optional note", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String notes;
}
