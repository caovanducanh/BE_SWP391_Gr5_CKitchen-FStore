package com.example.demologin.repository;

import com.example.demologin.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Integer> {

    @Query("select count(s) from StoreInventory s where s.quantity <= s.minStock")
    long countLowStockItems();

    @Query("select s from StoreInventory s where s.quantity <= s.minStock order by s.quantity asc")
    List<StoreInventory> findLowStockItems();
}
