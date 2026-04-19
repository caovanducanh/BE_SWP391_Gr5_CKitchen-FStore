package com.example.demologin.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPriorityConfigResponse {
    private Integer id;
    private String priorityCode;
    private Integer minDays;
    private Integer maxDays;
    private String description;
}
