package com.example.demologin.dto.request.shipper;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ScanPickupQrRequest {

    @NotBlank(message = "qrCode is required")
    @Schema(description = "Pickup QR code attached to delivery", example = "PK-ORD0421001-A1B2C3D4")
    private String qrCode;
}