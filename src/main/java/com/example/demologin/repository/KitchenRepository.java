package com.example.demologin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demologin.entity.Kitchen;

@Repository
public interface KitchenRepository extends JpaRepository<Kitchen, String> {
	Page<Kitchen> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Kitchen> findByStatus(String status, Pageable pageable);

	Page<Kitchen> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

	long countByStatus(String status);
}
