package com.example.demologin.repository;

import com.example.demologin.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Integer>, JpaSpecificationExecutor<StoreInventory> {

    @Query("select count(s) from StoreInventory s where s.quantity <= s.minStock")
    long countLowStockItems();

    @Query("select count(s) from StoreInventory s where s.store.id = :storeId and s.quantity <= s.minStock")
    long countLowStockItemsByStoreId(@Param("storeId") String storeId);

    @Query("select s from StoreInventory s where s.quantity <= s.minStock order by s.quantity asc")
    List<StoreInventory> findLowStockItems();
}

