package com.example.demologin.repository;

import com.example.demologin.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Optional<Delivery> findByOrder_Id(String orderId);
    Page<Delivery> findByOrder_Store_Id(String storeId, Pageable pageable);
    Page<Delivery> findByOrder_Store_IdAndStatus(String storeId, String status, Pageable pageable);
    long countByOrder_Store_IdAndStatusIn(String storeId, Collection<String> statuses);
}
