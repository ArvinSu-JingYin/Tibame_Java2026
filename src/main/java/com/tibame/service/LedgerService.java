package com.tibame.service;

import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.dto.RecordUpdateRequestDto;
import com.tibame.model.vo.MonthlySummaryVo;
import com.tibame.model.vo.RecordResponseVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LedgerService {
    RecordResponseVo createRecord(Long userId, RecordCreateRequestDto requestDto);
    RecordResponseVo quickCreateRecord(Long userId, String rawText);
    RecordResponseVo updateRecord(Long id, Long userId, RecordUpdateRequestDto requestDto);
    void deleteRecord(Long id, Long userId);
    RecordResponseVo getRecordById(Long id, Long userId);
    Page<RecordResponseVo> queryRecords(
            Long userId,
            String recordType,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Pageable pageable
    );
    MonthlySummaryVo getMonthlySummary(Long userId, int year, int month);
}
