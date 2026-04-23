package com.example.demologin.dto.request.centralkitchen;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class CompletePlanRequest {
    private String notes;
    private LocalDate expiryDate; // hạn dùng lô thành phẩm
}
