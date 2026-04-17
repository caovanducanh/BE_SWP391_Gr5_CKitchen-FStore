package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String manager;
    private String status;
    private LocalDate openDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
