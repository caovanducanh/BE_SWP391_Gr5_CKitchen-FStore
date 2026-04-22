package com.example.demologin.repository;

import com.example.demologin.entity.KitchenInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenInventoryRepository extends JpaRepository<KitchenInventory, Integer>, JpaSpecificationExecutor<KitchenInventory> {

    List<KitchenInventory> findByKitchenIsNull();

    long countByKitchenIsNull();

    boolean existsByKitchen_IdAndIngredient_IdAndBatchNo(String kitchenId, String ingredientId, String batchNo);

    boolean existsByKitchen_IdAndIngredient_IdAndBatchNoIsNull(String kitchenId, String ingredientId);

    boolean existsByKitchen_IdAndIngredient_IdAndBatchNoAndIdNot(String kitchenId, String ingredientId, String batchNo, Integer id);

    boolean existsByKitchen_IdAndIngredient_IdAndBatchNoIsNullAndIdNot(String kitchenId, String ingredientId, Integer id);

    @Query("select count(k) from KitchenInventory k where k.quantity <= k.minStock")
    long countLowStockItems();

    @Query("select k from KitchenInventory k where k.quantity <= k.minStock order by k.quantity asc")
    List<KitchenInventory> findLowStockItems();

    @Query("select distinct k.supplier from KitchenInventory k where k.supplier is not null and trim(k.supplier) <> '' order by k.supplier asc")
    List<String> findDistinctSuppliers();
}
