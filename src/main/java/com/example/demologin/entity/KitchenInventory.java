package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tổng hợp tồn kho nguyên liệu theo (kitchen, ingredient).
 * Chi tiết từng lô được lưu trong bảng ingredient_batches.
 * totalQuantity = sum(ingredient_batches.remaining_quantity) cho cặp (kitchen, ingredient).
 */
@Entity
@Table(name = "kitchen_inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_kitchen_inventory_kitchen_ingredient",
                columnNames = {"kitchen_id", "ingredient_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private Kitchen kitchen;

    /** Tổng số lượng còn lại (sum của tất cả lô ingredient_batches còn ACTIVE) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private Integer minStock;

    private LocalDateTime updatedAt;
}
