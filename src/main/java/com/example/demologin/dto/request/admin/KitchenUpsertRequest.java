package com.example.demologin.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KitchenUpsertRequest {

    @Size(max = 10, message = "Kitchen id must be at most 10 characters")
    private String id;

    @NotBlank(message = "Kitchen name must not be blank")
    @Size(max = 100, message = "Kitchen name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Kitchen address must not be blank")
    @Size(max = 255, message = "Kitchen address must be at most 255 characters")
    private String address;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Min(value = 1, message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotBlank(message = "Kitchen status must not be blank")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
    private String status;
}
