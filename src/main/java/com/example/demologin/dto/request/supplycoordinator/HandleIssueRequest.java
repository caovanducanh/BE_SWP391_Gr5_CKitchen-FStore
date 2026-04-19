package com.example.demologin.dto.request.supplycoordinator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class HandleIssueRequest {

    @NotBlank(message = "issueType is required")
    @Schema(description = "Issue type", allowableValues = {"SHORTAGE", "DELAY", "CANCELLATION", "OTHER"})
    private String issueType;

    @NotBlank(message = "description is required")
    @Schema(description = "Issue details and handling action")
    private String description;

    @Schema(description = "Set true to cancel the order immediately", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean cancelOrder;
}
