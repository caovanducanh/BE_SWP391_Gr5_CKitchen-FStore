package com.example.demologin.dto.request.supplycoordinator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateDeliveryStatusRequest {

    @NotBlank(message = "status is required")
    @Schema(description = "Delivery status", allowableValues = {"ASSIGNED", "SHIPPING", "DELAYED", "WAITING_CONFIRM", "DELIVERED", "CANCELLED"})
    private String status;

    @Schema(description = "Optional note from coordinator", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String notes;

    @Schema(description = "Receiver name, used when status is DELIVERED", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String receiverName;

    @Schema(description = "Temperature check result, used when status is DELIVERED", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private Boolean temperatureOk;
}
