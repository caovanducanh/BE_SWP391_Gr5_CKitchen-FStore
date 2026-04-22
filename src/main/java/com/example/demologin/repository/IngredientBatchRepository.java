package com.example.demologin.repository;

import com.example.demologin.entity.IngredientBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IngredientBatchRepository extends JpaRepository<IngredientBatch, String>,
        JpaSpecificationExecutor<IngredientBatch> {

    boolean existsByKitchen_IdAndIngredient_IdAndBatchNo(String kitchenId, String ingredientId, String batchNo);

    /** FEFO: lấy các lô còn hàng (ACTIVE) của 1 nguyên liệu trong bếp, sort theo hạn dùng sớm nhất trước */
    @Query("""
        select b from IngredientBatch b
        where b.kitchen.id = :kitchenId
          and b.ingredient.id = :ingredientId
          and b.status = 'ACTIVE'
          and b.remainingQuantity > 0
        order by b.expiryDate asc
        """)
    List<IngredientBatch> findActiveByKitchenAndIngredientOrderByExpiryAsc(
            @Param("kitchenId") String kitchenId,
            @Param("ingredientId") String ingredientId
    );

    /** Tổng số lượng còn lại của 1 nguyên liệu trong bếp (cross all active batches) */
    @Query("""
        select coalesce(sum(b.remainingQuantity), 0)
        from IngredientBatch b
        where b.kitchen.id = :kitchenId
          and b.ingredient.id = :ingredientId
          and b.status = 'ACTIVE'
        """)
    BigDecimal sumRemainingByKitchenAndIngredient(
            @Param("kitchenId") String kitchenId,
            @Param("ingredientId") String ingredientId
    );

    List<IngredientBatch> findByKitchen_IdAndIngredient_IdAndStatus(
            String kitchenId, String ingredientId, String status);
}
