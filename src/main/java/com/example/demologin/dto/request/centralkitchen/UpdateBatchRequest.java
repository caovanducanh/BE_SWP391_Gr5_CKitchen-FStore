package com.example.demologin.dto.request.centralkitchen;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateBatchRequest {
    private LocalDate expiryDate;
    private String status;
    private String notes;
}
