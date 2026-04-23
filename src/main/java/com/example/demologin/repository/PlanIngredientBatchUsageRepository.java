package com.example.demologin.repository;

import com.example.demologin.entity.PlanIngredientBatchUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanIngredientBatchUsageRepository extends JpaRepository<PlanIngredientBatchUsage, Integer> {

    List<PlanIngredientBatchUsage> findByPlan_Id(String planId);

    List<PlanIngredientBatchUsage> findByIngredientBatch_Id(String ingredientBatchId);
}
