package com.example.demologin.repository;

import com.example.demologin.entity.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, String> {
    long countByStatusIn(Collection<String> statuses);
    java.util.List<ProductionPlan> findByOrder_Id(String orderId);
}
