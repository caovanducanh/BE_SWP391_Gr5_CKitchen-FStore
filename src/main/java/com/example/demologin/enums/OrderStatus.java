package com.example.demologin.enums;

public enum OrderStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    PACKED_WAITING_SHIPPER,
    SHIPPING,
    DELIVERED,
    CANCELLED,

    // Legacy statuses kept for backward compatibility with existing DB rows
    PROCESSING,
    APPROVED
}
