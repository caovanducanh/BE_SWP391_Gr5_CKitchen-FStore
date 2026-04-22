package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientBatch {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private Kitchen kitchen;

    /** Số lô nguyên liệu (unique per kitchen+ingredient nếu cần) */
    @Column(nullable = false, length = 30)
    private String batchNo;

    /** Số lượng ban đầu khi nhập lô */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal initialQuantity;

    /** Số lượng còn lại (bị trừ dần khi dùng sản xuất) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingQuantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(length = 100)
    private String supplier;

    @Column(precision = 12, scale = 2)
    private BigDecimal importPrice;

    private LocalDate importDate;

    /** ACTIVE, DEPLETED, EXPIRED, DISPOSED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
