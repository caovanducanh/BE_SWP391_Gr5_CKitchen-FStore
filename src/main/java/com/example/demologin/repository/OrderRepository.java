package com.example.demologin.repository;

import com.example.demologin.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {
    long countByStatusIn(Collection<String> statuses);
    Page<Order> findByStore_Id(String storeId, Pageable pageable);
    Page<Order> findByStore_IdAndStatus(String storeId, String status, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
}

