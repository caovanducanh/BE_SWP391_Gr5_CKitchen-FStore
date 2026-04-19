package com.example.demologin.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPriorityConfigRequest {

    @NotBlank(message = "Priority code must not be blank")
    @Pattern(regexp = "HIGH|NORMAL|LOW", message = "Priority code must be HIGH, NORMAL, or LOW")
    private String priorityCode;

    @Min(value = 0, message = "minDays must be greater than or equal to 0")
    private Integer minDays;

    @Min(value = 0, message = "maxDays must be greater than or equal to 0")
    private Integer maxDays;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;
}
