package com.tibame.service.impl;

import com.tibame.common.exception.ApiException;
import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.dto.RecordUpdateRequestDto;
import com.tibame.model.entity.AccountRecord;
import com.tibame.model.entity.Category;
import com.tibame.model.vo.MonthlySummaryVo;
import com.tibame.model.vo.RecordResponseVo;
import com.tibame.repository.AccountRecordRepository;
import com.tibame.repository.CategoryRepository;
import com.tibame.service.LedgerService;
import com.tibame.service.SmartParserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final AccountRecordRepository accountRecordRepository;
    private final CategoryRepository categoryRepository;
    private final SmartParserService smartParserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordResponseVo createRecord(Long userId, RecordCreateRequestDto requestDto) {
        // 驗證分類是否屬於該用戶或為系統分類
        Category category = categoryRepository.findAvailableById(requestDto.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("指定的分類不存在或無權存取 (ID: " + requestDto.getCategoryId() + ")"));

        if (!category.getType().equalsIgnoreCase(requestDto.getRecordType())) {
            throw new ApiException("記帳類型與所選分類之收支類型不符");
        }

        AccountRecord record = AccountRecord.builder()
                .userId(userId)
                .categoryId(category.getId())
                .recordType(requestDto.getRecordType().toUpperCase())
                .amount(requestDto.getAmount())
                .description(requestDto.getDescription())
                .recordDate(requestDto.getRecordDate())
                .build();

        AccountRecord saved = accountRecordRepository.save(record);
        log.info("成功建立記帳紀錄: id={}, userId={}, amount={}, type={}",
                saved.getId(), userId, saved.getAmount(), saved.getRecordType());

        return convertToVo(saved, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordResponseVo quickCreateRecord(Long userId, String rawText) {
        RecordCreateRequestDto dto = smartParserService.parseQuickInput(rawText, userId);
        return createRecord(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordResponseVo updateRecord(Long id, Long userId, RecordUpdateRequestDto requestDto) {
        AccountRecord record = accountRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到記帳記錄 (ID: " + id + ")"));

        Category category = categoryRepository.findAvailableById(requestDto.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("指定的分類不存在或無權存取 (ID: " + requestDto.getCategoryId() + ")"));

        if (!category.getType().equalsIgnoreCase(requestDto.getRecordType())) {
            throw new ApiException("記帳類型與所選分類之收支類型不符");
        }

        record.setCategoryId(category.getId());
        record.setRecordType(requestDto.getRecordType().toUpperCase());
        record.setAmount(requestDto.getAmount());
        record.setDescription(requestDto.getDescription());
        record.setRecordDate(requestDto.getRecordDate());

        AccountRecord updated = accountRecordRepository.save(record);
        log.info("成功更新記帳紀錄: id={}, userId={}", updated.getId(), userId);

        return convertToVo(updated, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        AccountRecord record = accountRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到記帳記錄 (ID: " + id + ")"));

        accountRecordRepository.delete(record);
        log.info("成功刪除記帳紀錄: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecordResponseVo getRecordById(Long id, Long userId) {
        AccountRecord record = accountRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到記帳記錄 (ID: " + id + ")"));

        Category category = categoryRepository.findById(record.getCategoryId()).orElse(null);
        return convertToVo(record, category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponseVo> queryRecords(
            Long userId,
            String recordType,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Pageable pageable) {

        Specification<AccountRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 嚴格使用者隔離
            predicates.add(cb.equal(root.get("userId"), userId));

            if (StringUtils.hasText(recordType)) {
                predicates.add(cb.equal(root.get("recordType"), recordType.toUpperCase()));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordDate"), endDate));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.like(root.get("description"), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 預設依記帳日期倒序、主鍵倒序排列
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "recordDate").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<AccountRecord> page = accountRecordRepository.findAll(spec, sortedPageable);

        // 批次取得分類資訊以豐富 VO
        Set<Long> categoryIds = page.getContent().stream()
                .map(AccountRecord::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
                categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));

        return page.map(record -> convertToVo(record, categoryMap.get(record.getCategoryId())));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryVo getMonthlySummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal totalIncome = accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "INCOME", startDate, endDate);
        BigDecimal totalExpense = accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "EXPENSE", startDate, endDate);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        return MonthlySummaryVo.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();
    }

    private RecordResponseVo convertToVo(AccountRecord record, Category category) {
        return RecordResponseVo.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .categoryId(record.getCategoryId())
                .categoryName(category != null ? category.getName() : "未分類")
                .categoryIconCode(category != null ? category.getIconCode() : "tag")
                .recordType(record.getRecordType())
                .amount(record.getAmount())
                .description(record.getDescription())
                .recordDate(record.getRecordDate())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
