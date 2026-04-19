package com.example.demologin.dto.request.supplycoordinator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleDeliveryRequest {

    @NotBlank(message = "orderId is required")
    @Schema(description = "Order ID to schedule delivery", example = "ORD0419001")
    private String orderId;

    @Schema(description = "Delivery status when scheduling", allowableValues = {"ASSIGNED", "SHIPPING"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String status;

    @Schema(description = "Planned assigned time. If null, backend uses now()", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private LocalDateTime assignedAt;

    @Schema(description = "Optional note for delivery", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String notes;
}
