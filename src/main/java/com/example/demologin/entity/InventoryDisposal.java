package com.example.demologin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_disposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDisposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String inventoryType;

    @Column(nullable = false)
    private Integer itemId;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityDisposed;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 200)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disposed_by", nullable = false, referencedColumnName = "userId")
    private User disposedBy;

    @Column(nullable = false)
    private LocalDateTime disposedAt;
}
