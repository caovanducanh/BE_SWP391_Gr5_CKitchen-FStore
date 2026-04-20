package com.example.demologin.repository;

import com.example.demologin.entity.SaleItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    List<SaleItem> findBySaleId(String saleId);

    Page<SaleItem> findBySaleId(String saleId, Pageable pageable);

    List<SaleItem> findBySaleIdOrderByIdAsc(String saleId);

    long countBySaleId(String saleId);

    @Query("select coalesce(sum(si.quantity), 0) from SaleItem si where si.sale.id = :saleId")
    Integer sumQuantityBySaleId(@Param("saleId") String saleId);

    void deleteBySaleId(String saleId);
}
