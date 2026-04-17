package com.example.demologin.dto.request.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ConfirmReceiptRequest {

    @NotBlank(message = "receiverName is required")
    private String receiverName;

    @NotNull(message = "temperatureOk is required")
    private Boolean temperatureOk;

    private String notes;
}
