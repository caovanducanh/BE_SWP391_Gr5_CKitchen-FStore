package com.example.demologin.repository;

import com.example.demologin.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    long countByStatusIn(Collection<String> statuses);
}
