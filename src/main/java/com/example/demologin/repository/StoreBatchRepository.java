package com.example.demologin.repository;

import com.example.demologin.entity.StoreBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreBatchRepository extends JpaRepository<StoreBatch, Integer>, JpaSpecificationExecutor<StoreBatch> {
    List<StoreBatch> findByStoreId(String storeId);
    List<StoreBatch> findByStoreIdAndProductId(String storeId, String productId);
    Optional<StoreBatch> findByStoreIdAndProductIdAndBatchId(String storeId, String productId, String batchId);
}
