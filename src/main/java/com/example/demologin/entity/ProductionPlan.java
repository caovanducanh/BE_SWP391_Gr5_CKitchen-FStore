package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionPlan {

    @Id
    @Column(length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id")
    private Kitchen kitchen;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    /**
     * Status flow: DRAFT -> APPROVED -> IN_PRODUCTION -> COMPLETED | CANCELLED
     */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(length = 100)
    private String staff;

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
