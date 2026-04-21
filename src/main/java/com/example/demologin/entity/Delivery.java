package com.example.demologin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @Column(length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinator_id", nullable = false, referencedColumnName = "userId")
    private User coordinator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", referencedColumnName = "userId")
    private User shipper;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "pickup_qr_code", unique = true, length = 120)
    private String pickupQrCode;

    private LocalDateTime pickedUpAt;

    private LocalDateTime deliveredAt;

    @Column(length = 500)
    private String notes;

    @Column(length = 100)
    private String receiverName;

    private Boolean temperatureOk;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
