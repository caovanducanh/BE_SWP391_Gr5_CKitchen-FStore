package com.example.demologin.dto.request.shipper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class MarkDeliverySuccessRequest {

    @Schema(description = "Optional note from shipper when delivery is handed off to store", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String notes;
}
