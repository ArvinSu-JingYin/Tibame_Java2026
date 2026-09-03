package com.tibame.service;

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
import com.tibame.service.impl.LedgerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRecordRepository accountRecordRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SmartParserService smartParserService;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    // ==========================================
    // 1. createRecord 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常建立支出記帳 (EXPENSE)")
    void testCreateRecord_Expense_Success() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 10L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type("EXPENSE")
                .name("外食餐飲")
                .iconCode("food")
                .build();

        RecordCreateRequestDto request = RecordCreateRequestDto.builder()
                .categoryId(categoryId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("150.00"))
                .description("商務午餐")
                .recordDate(LocalDate.of(2026, 9, 2))
                .build();

        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.of(category));
        when(accountRecordRepository.save(any(AccountRecord.class))).thenAnswer(invocation -> {
            AccountRecord entity = invocation.getArgument(0);
            entity.setId(100L);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        // Act
        RecordResponseVo vo = ledgerService.createRecord(userId, request);

        // Assert
        assertNotNull(vo);
        assertEquals(100L, vo.getId());
        assertEquals("EXPENSE", vo.getRecordType());
        assertEquals(new BigDecimal("150.00"), vo.getAmount());
        assertEquals("外食餐飲", vo.getCategoryName());
        assertEquals("food", vo.getCategoryIconCode());
        verify(accountRecordRepository, times(1)).save(any(AccountRecord.class));
    }

    @Test
    @DisplayName("測試正常建立收入記帳 (INCOME)")
    void testCreateRecord_Income_Success() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 20L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(null)
                .type("INCOME")
                .name("每月薪資")
                .iconCode("wallet")
                .isSystem(true)
                .build();

        RecordCreateRequestDto request = RecordCreateRequestDto.builder()
                .categoryId(categoryId)
                .recordType("INCOME")
                .amount(new BigDecimal("60000.00"))
                .description("9月份本薪")
                .recordDate(LocalDate.of(2026, 9, 1))
                .build();

        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.of(category));
        when(accountRecordRepository.save(any(AccountRecord.class))).thenAnswer(invocation -> {
            AccountRecord entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        // Act
        RecordResponseVo vo = ledgerService.createRecord(userId, request);

        // Assert
        assertNotNull(vo);
        assertEquals("INCOME", vo.getRecordType());
        assertEquals(new BigDecimal("60000.00"), vo.getAmount());
        assertEquals("每月薪資", vo.getCategoryName());
    }

    @Test
    @DisplayName("測試指定不存在或無權存取的分類 ID 應拋出 ResourceNotFoundException")
    void testCreateRecord_CategoryNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 999L;
        RecordCreateRequestDto request = RecordCreateRequestDto.builder()
                .categoryId(categoryId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("200"))
                .recordDate(LocalDate.now())
                .build();

        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ledgerService.createRecord(userId, request));
        verify(accountRecordRepository, never()).save(any(AccountRecord.class));
    }

    @Test
    @DisplayName("測試記帳類型與分類收支類型不符應拋出 ApiException")
    void testCreateRecord_TypeMismatch_ThrowsApiException() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 10L;
        Category expenseCategory = Category.builder()
                .id(categoryId)
                .type("EXPENSE")
                .name("餐飲")
                .build();

        RecordCreateRequestDto request = RecordCreateRequestDto.builder()
                .categoryId(categoryId)
                .recordType("INCOME") // 故意傳入 INCOME 與 EXPENSE 分類衝突
                .amount(new BigDecimal("500"))
                .recordDate(LocalDate.now())
                .build();

        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.of(expenseCategory));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.createRecord(userId, request));
        assertEquals("記帳類型與所選分類之收支類型不符", exception.getMessage());
        verify(accountRecordRepository, never()).save(any(AccountRecord.class));
    }

    // ==========================================
    // 2. quickCreateRecord 情境測試
    // ==========================================

    @Test
    @DisplayName("測試快速文字記帳 (代理 SmartParser 解析後建立記錄)")
    void testQuickCreateRecord_DelegatesToSmartParserAndCreatesRecord() {
        // Arrange
        Long userId = 1L;
        String rawText = "午餐 120";
        RecordCreateRequestDto parsedDto = RecordCreateRequestDto.builder()
                .categoryId(10L)
                .recordType("EXPENSE")
                .amount(new BigDecimal("120.00"))
                .description("午餐")
                .recordDate(LocalDate.now())
                .build();

        Category category = Category.builder()
                .id(10L)
                .type("EXPENSE")
                .name("餐飲食品")
                .iconCode("food")
                .build();

        when(smartParserService.parseQuickInput(rawText, userId)).thenReturn(parsedDto);
        when(categoryRepository.findAvailableById(10L, userId)).thenReturn(Optional.of(category));
        when(accountRecordRepository.save(any(AccountRecord.class))).thenAnswer(invocation -> {
            AccountRecord r = invocation.getArgument(0);
            r.setId(105L);
            return r;
        });

        // Act
        RecordResponseVo vo = ledgerService.quickCreateRecord(userId, rawText);

        // Assert
        assertNotNull(vo);
        assertEquals(105L, vo.getId());
        assertEquals(new BigDecimal("120.00"), vo.getAmount());
        assertEquals("餐飲食品", vo.getCategoryName());
        verify(smartParserService, times(1)).parseQuickInput(rawText, userId);
    }

    // ==========================================
    // 3. updateRecord 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常更新記帳金額、分類、日期與備註")
    void testUpdateRecord_Success() {
        // Arrange
        Long recordId = 50L;
        Long userId = 1L;
        Long newCategoryId = 12L;

        AccountRecord existingRecord = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .categoryId(10L)
                .recordType("EXPENSE")
                .amount(new BigDecimal("100.00"))
                .description("舊備註")
                .recordDate(LocalDate.of(2026, 9, 1))
                .build();

        Category newCategory = Category.builder()
                .id(newCategoryId)
                .type("EXPENSE")
                .name("交通運輸")
                .iconCode("car")
                .build();

        RecordUpdateRequestDto request = RecordUpdateRequestDto.builder()
                .categoryId(newCategoryId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("250.00"))
                .description("高鐵車票")
                .recordDate(LocalDate.of(2026, 9, 2))
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(existingRecord));
        when(categoryRepository.findAvailableById(newCategoryId, userId)).thenReturn(Optional.of(newCategory));
        when(accountRecordRepository.save(any(AccountRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RecordResponseVo vo = ledgerService.updateRecord(recordId, userId, request);

        // Assert
        assertNotNull(vo);
        assertEquals(new BigDecimal("250.00"), vo.getAmount());
        assertEquals("高鐵車票", vo.getDescription());
        assertEquals("交通運輸", vo.getCategoryName());
        assertEquals("car", vo.getCategoryIconCode());
        verify(accountRecordRepository).save(existingRecord);
    }

    @Test
    @DisplayName("測試嘗試更新不存在或非本人之記帳記錄應拋出 ResourceNotFoundException")
    void testUpdateRecord_NotFoundOrOtherUser_ThrowsResourceNotFoundException() {
        // Arrange
        Long recordId = 999L;
        Long userId = 1L;
        RecordUpdateRequestDto request = RecordUpdateRequestDto.builder()
                .categoryId(10L)
                .recordType("EXPENSE")
                .amount(new BigDecimal("100"))
                .recordDate(LocalDate.now())
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ledgerService.updateRecord(recordId, userId, request));
        verify(accountRecordRepository, never()).save(any(AccountRecord.class));
    }

    @Test
    @DisplayName("測試更新時指定不存在之分類 ID 應拋出 ResourceNotFoundException")
    void testUpdateRecord_CategoryNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long recordId = 50L;
        Long userId = 1L;
        Long nonExistentCatId = 999L;

        AccountRecord existingRecord = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .categoryId(10L)
                .recordType("EXPENSE")
                .build();

        RecordUpdateRequestDto request = RecordUpdateRequestDto.builder()
                .categoryId(nonExistentCatId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("100"))
                .recordDate(LocalDate.now())
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(existingRecord));
        when(categoryRepository.findAvailableById(nonExistentCatId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ledgerService.updateRecord(recordId, userId, request));
        verify(accountRecordRepository, never()).save(any(AccountRecord.class));
    }

    @Test
    @DisplayName("測試更新時記帳類型與新分類類型不一致應拋出 ApiException")
    void testUpdateRecord_TypeMismatch_ThrowsApiException() {
        // Arrange
        Long recordId = 50L;
        Long userId = 1L;
        Long incomeCatId = 20L;

        AccountRecord existingRecord = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .categoryId(10L)
                .recordType("EXPENSE")
                .build();

        Category incomeCategory = Category.builder()
                .id(incomeCatId)
                .type("INCOME")
                .name("薪資")
                .build();

        RecordUpdateRequestDto request = RecordUpdateRequestDto.builder()
                .categoryId(incomeCatId)
                .recordType("EXPENSE") // 欲更新為 EXPENSE，但分類卻是 INCOME
                .amount(new BigDecimal("100"))
                .recordDate(LocalDate.now())
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(existingRecord));
        when(categoryRepository.findAvailableById(incomeCatId, userId)).thenReturn(Optional.of(incomeCategory));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.updateRecord(recordId, userId, request));
        assertEquals("記帳類型與所選分類之收支類型不符", exception.getMessage());
        verify(accountRecordRepository, never()).save(any(AccountRecord.class));
    }

    // ==========================================
    // 4. deleteRecord 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常刪除自己的記帳記錄")
    void testDeleteRecord_Success() {
        // Arrange
        Long recordId = 50L;
        Long userId = 1L;
        AccountRecord record = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(record));

        // Act
        ledgerService.deleteRecord(recordId, userId);

        // Assert
        verify(accountRecordRepository, times(1)).delete(record);
    }

    @Test
    @DisplayName("測試嘗試刪除不存在或非本人之記帳記錄應拋出 ResourceNotFoundException")
    void testDeleteRecord_NotFoundOrOtherUser_ThrowsResourceNotFoundException() {
        // Arrange
        Long recordId = 999L;
        Long userId = 1L;
        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ledgerService.deleteRecord(recordId, userId));
        verify(accountRecordRepository, never()).delete(any(AccountRecord.class));
    }

    // ==========================================
    // 5. getRecordById 情境測試
    // ==========================================

    @Test
    @DisplayName("測試查詢單筆記帳 (含關聯分類資訊富化)")
    void testGetRecordById_EnrichesCategoryInfo_Success() {
        // Arrange
        Long recordId = 100L;
        Long userId = 1L;
        Long categoryId = 10L;

        AccountRecord record = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .categoryId(categoryId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("300.00"))
                .description("聚餐")
                .recordDate(LocalDate.of(2026, 9, 1))
                .build();

        Category category = Category.builder()
                .id(categoryId)
                .name("餐飲食品")
                .iconCode("food")
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(record));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act
        RecordResponseVo vo = ledgerService.getRecordById(recordId, userId);

        // Assert
        assertNotNull(vo);
        assertEquals(recordId, vo.getId());
        assertEquals("餐飲食品", vo.getCategoryName());
        assertEquals("food", vo.getCategoryIconCode());
    }

    @Test
    @DisplayName("測試查詢單筆記帳 (分類為 null 孤兒記錄時安全 fallback)")
    void testGetRecordById_OrphanRecordWithNullCategory_FallbackToDefault() {
        // Arrange
        Long recordId = 100L;
        Long userId = 1L;
        Long orphanCatId = 999L;

        AccountRecord record = AccountRecord.builder()
                .id(recordId)
                .userId(userId)
                .categoryId(orphanCatId)
                .recordType("EXPENSE")
                .amount(new BigDecimal("300.00"))
                .description("已刪分類之孤兒記錄")
                .recordDate(LocalDate.now())
                .build();

        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(record));
        when(categoryRepository.findById(orphanCatId)).thenReturn(Optional.empty());

        // Act
        RecordResponseVo vo = ledgerService.getRecordById(recordId, userId);

        // Assert
        assertNotNull(vo);
        assertEquals("未分類", vo.getCategoryName());
        assertEquals("tag", vo.getCategoryIconCode());
    }

    @Test
    @DisplayName("測試查詢不存在或他人之記帳記錄應拋出 ResourceNotFoundException")
    void testGetRecordById_NotFoundOrOtherUser_ThrowsResourceNotFoundException() {
        // Arrange
        Long recordId = 999L;
        Long userId = 1L;
        when(accountRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ledgerService.getRecordById(recordId, userId));
    }

    // ==========================================
    // 6. queryRecords 情境測試
    // ==========================================

    @Test
    @DisplayName("測試多條件 Specification 動態查詢分頁與批次分類富化")
    @SuppressWarnings("unchecked")
    void testQueryRecords_WithPaginationAndBatchEnrichment_Success() {
        // Arrange
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        AccountRecord rec1 = AccountRecord.builder()
                .id(1L)
                .userId(userId)
                .categoryId(10L)
                .recordType("EXPENSE")
                .amount(new BigDecimal("100.00"))
                .recordDate(LocalDate.of(2026, 9, 2))
                .build();
        AccountRecord rec2 = AccountRecord.builder()
                .id(2L)
                .userId(userId)
                .categoryId(20L)
                .recordType("INCOME")
                .amount(new BigDecimal("5000.00"))
                .recordDate(LocalDate.of(2026, 9, 1))
                .build();

        Page<AccountRecord> mockPage = new PageImpl<>(List.of(rec1, rec2), pageable, 2);

        Category cat1 = Category.builder().id(10L).name("餐飲").iconCode("food").build();
        Category cat2 = Category.builder().id(20L).name("薪資").iconCode("wallet").build();

        when(accountRecordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);
        when(categoryRepository.findAllById(anySet()))
                .thenReturn(List.of(cat1, cat2));

        // Act
        Page<RecordResponseVo> result = ledgerService.queryRecords(
                userId, "EXPENSE", 10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "午餐", pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("餐飲", result.getContent().get(0).getCategoryName());
        assertEquals("薪資", result.getContent().get(1).getCategoryName());
        verify(accountRecordRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==========================================
    // 7. getMonthlySummary 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常年月統計 (收入、支出、淨結餘計算)")
    void testGetMonthlySummary_CalculatesNetBalance_Success() {
        // Arrange
        Long userId = 1L;
        int year = 2026;
        int month = 8;
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);

        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "INCOME", startDate, endDate)).thenReturn(new BigDecimal("60000.00"));
        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "EXPENSE", startDate, endDate)).thenReturn(new BigDecimal("25000.00"));

        // Act
        MonthlySummaryVo summary = ledgerService.getMonthlySummary(userId, year, month);

        // Assert
        assertNotNull(summary);
        assertEquals(year, summary.getYear());
        assertEquals(month, summary.getMonth());
        assertEquals(new BigDecimal("60000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("25000.00"), summary.getTotalExpense());
        assertEquals(new BigDecimal("35000.00"), summary.getNetBalance());
    }

    @Test
    @DisplayName("測試當月無任何收支 (DB 回傳 null 金額時安全處理為 0)")
    void testGetMonthlySummary_WithNullAmounts_SafelyDefaultsToZero() {
        // Arrange
        Long userId = 1L;
        int year = 2026;
        int month = 7;
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);

        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "INCOME", startDate, endDate)).thenReturn(null);
        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "EXPENSE", startDate, endDate)).thenReturn(null);

        // Act
        MonthlySummaryVo summary = ledgerService.getMonthlySummary(userId, year, month);

        // Assert
        assertNotNull(summary);
        assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
        assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
        assertEquals(BigDecimal.ZERO, summary.getNetBalance());
    }

    @Test
    @DisplayName("測試閏月與月份邊界計算 (2月閏年與平年邊界日期)")
    void testGetMonthlySummary_CalculatesLeapYearAndDateBoundaries() {
        // Arrange (閏年 2024年2月: 2/1 ~ 2/29)
        Long userId = 1L;
        int leapYear = 2024;
        int month = 2;
        LocalDate leapStart = LocalDate.of(2024, 2, 1);
        LocalDate leapEnd = LocalDate.of(2024, 2, 29);

        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "INCOME", leapStart, leapEnd)).thenReturn(new BigDecimal("1000"));
        when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
                userId, "EXPENSE", leapStart, leapEnd)).thenReturn(new BigDecimal("500"));

        // Act
        MonthlySummaryVo leapSummary = ledgerService.getMonthlySummary(userId, leapYear, month);

        // Assert
        assertNotNull(leapSummary);
        assertEquals(new BigDecimal("500"), leapSummary.getNetBalance());

        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(accountRecordRepository, times(1)).sumAmountByUserIdAndRecordTypeAndDateRange(
                eq(userId), eq("INCOME"), eq(leapStart), endCaptor.capture());
        assertEquals(leapEnd, endCaptor.getValue());
    }
}
