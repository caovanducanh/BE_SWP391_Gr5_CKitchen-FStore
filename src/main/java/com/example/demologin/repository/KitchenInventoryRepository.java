package com.example.demologin.repository;

import com.example.demologin.entity.KitchenInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenInventoryRepository extends JpaRepository<KitchenInventory, Integer>,
        JpaSpecificationExecutor<KitchenInventory> {

    Optional<KitchenInventory> findByKitchen_IdAndIngredient_Id(String kitchenId, String ingredientId);

    boolean existsByKitchen_IdAndIngredient_Id(String kitchenId, String ingredientId);

    @Query("select count(k) from KitchenInventory k where k.totalQuantity <= k.minStock")
    long countLowStockItems();

    @Query("select k from KitchenInventory k where k.totalQuantity <= k.minStock order by k.totalQuantity asc")
    List<KitchenInventory> findLowStockItems();

    List<KitchenInventory> findByKitchen_Id(String kitchenId);

    @Query("""
        select count(k) from KitchenInventory k
        where k.kitchen.id = :kitchenId and k.totalQuantity <= k.minStock
        """)
    long countLowStockItemsByKitchenId(@Param("kitchenId") String kitchenId);

    @Modifying
    @Query("delete from KitchenInventory k where k.ingredient.id in :ids")
    void deleteByIngredient_IdIn(@Param("ids") List<String> ids);
}
