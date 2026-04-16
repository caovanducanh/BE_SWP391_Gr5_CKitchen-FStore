package com.example.demologin.repository;

import com.example.demologin.entity.InventoryDisposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InventoryDisposalRepository extends JpaRepository<InventoryDisposal, Integer> {

    @Query("select coalesce(sum(i.quantityDisposed), 0) from InventoryDisposal i")
    BigDecimal sumDisposedQuantity();
}
