package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lô thành phẩm được tạo ra sau khi hoàn thành kế hoạch sản xuất.
 * Từ đây thành phẩm được phân phối vào StoreInventory của các cửa hàng.
 */
@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private Kitchen kitchen;

    /** Số lượng sản xuất */
    @Column(nullable = false)
    private Integer quantity;

    /** Số lượng còn lại chưa phân phối ra cửa hàng */
    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false, length = 20)
    private String unit;

    /** Hạn dùng của lô thành phẩm */
    private LocalDate expiryDate;

    /** AVAILABLE, PARTIALLY_DISTRIBUTED, FULLY_DISTRIBUTED */
    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 100)
    private String staff;

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
