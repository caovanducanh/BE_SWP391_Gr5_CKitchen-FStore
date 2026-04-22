package com.example.demologin.repository;

import com.example.demologin.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String>, JpaSpecificationExecutor<Batch> {

    long countByStatusIn(Collection<String> statuses);

    List<Batch> findByPlan_Id(String planId);

    List<Batch> findByProduct_IdAndStatusIn(String productId, Collection<String> statuses);
}
