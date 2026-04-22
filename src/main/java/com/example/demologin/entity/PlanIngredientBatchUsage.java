package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks how much of each ingredient_batch was consumed by a production plan.
 * Enables full FEFO traceability: "plan X used ingredient batch Y (qty Z)".
 */
@Entity
@Table(name = "plan_ingredient_batch_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanIngredientBatchUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_batch_id", nullable = false)
    private IngredientBatch ingredientBatch;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityUsed;

    private LocalDateTime createdAt;
}
