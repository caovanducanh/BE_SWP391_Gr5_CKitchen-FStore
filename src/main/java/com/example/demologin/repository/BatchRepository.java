package com.example.demologin.repository;

import com.example.demologin.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {
    long countByStatusIn(Collection<String> statuses);
}
