package com.example.demologin.dto.request.supplycoordinator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AssignOrderKitchenRequest {

    @NotBlank(message = "kitchenId is required")
    @Schema(description = "Kitchen ID to assign order", example = "KIT001")
    private String kitchenId;

    @Schema(description = "Optional note from coordinator", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String notes;
}
