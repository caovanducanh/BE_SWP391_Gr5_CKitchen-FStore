package com.example.demologin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demologin.entity.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, String>, JpaSpecificationExecutor<Store> {
	Page<Store> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Store> findByStatus(String status, Pageable pageable);

	Page<Store> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

	long countByStatus(String status);
}
