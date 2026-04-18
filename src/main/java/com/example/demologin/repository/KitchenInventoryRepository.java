package com.example.demologin.repository;

import com.example.demologin.entity.KitchenInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenInventoryRepository extends JpaRepository<KitchenInventory, Integer>, JpaSpecificationExecutor<KitchenInventory> {

    @Query("select count(k) from KitchenInventory k where k.quantity <= k.minStock")
    long countLowStockItems();

    @Query("select k from KitchenInventory k where k.quantity <= k.minStock order by k.quantity asc")
    List<KitchenInventory> findLowStockItems();
}
