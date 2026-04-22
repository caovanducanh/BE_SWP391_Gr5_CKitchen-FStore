package com.example.demologin.dto.response.manager;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ManagerKitchenInventoryGroupResponse {
    private String kitchenId;
    private String kitchenName;
    private List<ManagerKitchenInventoryItemResponse> items;
}
