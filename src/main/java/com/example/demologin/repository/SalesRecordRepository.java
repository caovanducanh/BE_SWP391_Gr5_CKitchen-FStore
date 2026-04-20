package com.example.demologin.repository;

import com.example.demologin.entity.SalesRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, String> {

    @Query("select coalesce(sum(s.totalRevenue), 0) from SalesRecord s")
    BigDecimal sumTotalRevenue();

    @Query("""
            select coalesce(sum(s.totalRevenue), 0)
            from SalesRecord s
            where (:storeId is null or s.store.id = :storeId)
              and (:fromDate is null or s.date >= :fromDate)
              and (:toDate is null or s.date <= :toDate)
            """)
    BigDecimal sumRevenueByStoreAndDateRange(@Param("storeId") String storeId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query("""
            select s.store.id as storeId,
                   s.store.name as storeName,
                   coalesce(sum(s.totalRevenue), 0) as totalRevenue
            from SalesRecord s
            where (:fromDate is null or s.date >= :fromDate)
              and (:toDate is null or s.date <= :toDate)
            group by s.store.id, s.store.name
            order by s.store.id asc
            """)
    List<StoreRevenueProjection> summarizeRevenueByStore(@Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate);

    Optional<SalesRecord> findByStoreIdAndDate(String storeId, LocalDate date);

    boolean existsByStoreIdAndDate(String storeId, LocalDate date);

    @Query("""
            select s from SalesRecord s
            where s.store.id = :storeId
              and (:fromDate is null or s.date >= :fromDate)
              and (:toDate is null or s.date <= :toDate)
            order by s.date desc
            """)
    List<SalesRecord> findByStoreIdAndDateRange(@Param("storeId") String storeId,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);

    @Query("""
            select s from SalesRecord s
            where s.store.id = :storeId
              and (:fromDate is null or s.date >= :fromDate)
              and (:toDate is null or s.date <= :toDate)
            """)
    Page<SalesRecord> findByStoreIdAndDateRange(@Param("storeId") String storeId,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate,
                                                Pageable pageable);

    @Query("select s.id from SalesRecord s")
    List<String> findAllIds();

    @Query("""
            select s.date as reportDate,
                   s.store.id as storeId,
                   s.store.name as storeName,
                   sum(s.totalRevenue) as totalRevenue,
                   count(s.id) as reportCount
            from SalesRecord s
            where (:fromDate is null or s.date >= :fromDate)
              and (:toDate is null or s.date <= :toDate)
              and (:storeId is null or s.store.id = :storeId)
            group by s.date, s.store.id, s.store.name
            order by s.date desc, s.store.id asc
            """)
    List<DailyRevenueProjection> summarizeDailyRevenue(@Param("fromDate") LocalDate fromDate,
                                                       @Param("toDate") LocalDate toDate,
                                                       @Param("storeId") String storeId);

    interface DailyRevenueProjection {
        LocalDate getReportDate();

        String getStoreId();

        String getStoreName();

        BigDecimal getTotalRevenue();

        Long getReportCount();
    }

      interface StoreRevenueProjection {
        String getStoreId();

        String getStoreName();

        BigDecimal getTotalRevenue();
      }
}
