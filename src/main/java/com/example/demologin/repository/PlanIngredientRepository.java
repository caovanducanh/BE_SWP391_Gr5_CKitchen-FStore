package com.example.demologin.repository;

import com.example.demologin.entity.PlanIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanIngredientRepository extends JpaRepository<PlanIngredient, Integer> {
    List<PlanIngredient> findByPlan_Id(String planId);
}
