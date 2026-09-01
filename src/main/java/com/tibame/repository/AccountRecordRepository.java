package com.tibame.repository;

import com.tibame.model.entity.AccountRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AccountRecordRepository extends JpaRepository<AccountRecord, Long>, JpaSpecificationExecutor<AccountRecord> {

    Optional<AccountRecord> findByIdAndUserId(Long id, Long userId);

    long countByCategoryId(Long categoryId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM AccountRecord r WHERE r.userId = :userId AND r.recordType = :recordType AND r.recordDate >= :startDate AND r.recordDate <= :endDate")
    BigDecimal sumAmountByUserIdAndRecordTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("recordType") String recordType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
