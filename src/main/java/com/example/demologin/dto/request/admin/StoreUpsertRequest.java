package com.example.demologin.dto.request.admin;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreUpsertRequest {

    @Size(max = 10, message = "Store id must be at most 10 characters")
    private String id;

    @NotBlank(message = "Store name must not be blank")
    @Size(max = 100, message = "Store name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Store address must not be blank")
    @Size(max = 255, message = "Store address must be at most 255 characters")
    private String address;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 100, message = "Manager name must be at most 100 characters")
    private String manager;

    @NotBlank(message = "Store status must not be blank")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
    private String status;

    private LocalDate openDate;
}
