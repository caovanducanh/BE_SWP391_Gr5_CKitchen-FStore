package com.example.demologin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demologin.entity.OrderPriorityConfig;

@Repository
public interface OrderPriorityConfigRepository extends JpaRepository<OrderPriorityConfig, Integer> {
	Optional<OrderPriorityConfig> findByPriorityCode(String priorityCode);

	boolean existsByPriorityCode(String priorityCode);
}
