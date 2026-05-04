package com.example.demologin.repository;

import com.example.demologin.entity.OrderItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemBatchRepository extends JpaRepository<OrderItemBatch, Integer> {
    List<OrderItemBatch> findByOrderItem_Id(Integer orderItemId);
    List<OrderItemBatch> findByOrderItem_Order_Id(String orderId);
}
