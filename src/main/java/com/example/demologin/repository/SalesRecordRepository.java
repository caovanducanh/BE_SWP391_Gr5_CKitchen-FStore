package com.example.demologin.repository;

import com.example.demologin.entity.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, String> {

    @Query("select coalesce(sum(s.totalRevenue), 0) from SalesRecord s")
    BigDecimal sumTotalRevenue();
}
