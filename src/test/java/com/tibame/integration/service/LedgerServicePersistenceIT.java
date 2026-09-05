package com.tibame.integration.service;

import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.integration.base.ServiceIntegrationTestBase;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.dto.RecordUpdateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.model.entity.User;
import com.tibame.model.vo.MonthlySummaryVo;
import com.tibame.model.vo.RecordResponseVo;
import com.tibame.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 流水帳業務服務持久化整合測試
 * 驗證真實 JPA Specification 動態查詢、JPQL COALESCE/SUM 聚合運算、外鍵邊界與跨租戶存取隔離
 */
@DisplayName("流水帳業務服務持久化整合測試 (LedgerServicePersistenceIT)")
class LedgerServicePersistenceIT extends ServiceIntegrationTestBase {

    @Autowired
    private LedgerService ledgerService;

    @Test
    @DisplayName("TC-LEDGER-IT-01: 月度收支 JPQL 聚合計算精準度驗證 (COALESCE/SUM 跨月份過濾)")
    void testMonthlySummaryAggregationPrecision() {
        User user = createAndPersistTestUser();
        Category salaryCategory = categoryRepository.findAvailableCategoriesByType(user.getId(), "INCOME").get(0);
        Category foodCategory = categoryRepository.findAvailableCategoriesByType(user.getId(), "EXPENSE").get(0);

        // 2 筆收入 (50,000, 10,000)
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(salaryCategory.getId())
                .recordType("INCOME")
                .amount(new BigDecimal("50000.00"))
                .description("本月本薪")
                .recordDate(LocalDate.of(2026, 5, 5))
                .build());
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(salaryCategory.getId())
                .recordType("INCOME")
                .amount(new BigDecimal("10000.00"))
                .description("專案績效獎金")
                .recordDate(LocalDate.of(2026, 5, 20))
                .build());

        // 3 筆支出 (12,000, 3,000, 1,500)
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("12000.00"))
                .description("月度餐費預支")
                .recordDate(LocalDate.of(2026, 5, 1))
                .build());
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("3000.00"))
                .description("部門慶功聚餐")
                .recordDate(LocalDate.of(2026, 5, 15))
                .build());
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("1500.00"))
                .description("週末採買食材")
                .recordDate(LocalDate.of(2026, 5, 28))
                .build());

        // 1 筆次月支出 (5,000) - 應被精準排除
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("5000.00"))
                .description("次月預付定金")
                .recordDate(LocalDate.of(2026, 6, 2))
                .build());

        MonthlySummaryVo summary = ledgerService.getMonthlySummary(user.getId(), 2026, 5);

        assertThat(summary).isNotNull();
        assertThat(summary.getYear()).isEqualTo(2026);
        assertThat(summary.getMonth()).isEqualTo(5);
        assertThat(summary.getTotalIncome())
                .as("總收入應精準為 60,000.00")
                .isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(summary.getTotalExpense())
                .as("總支出應精準為 16,500.00 (排除次月 5,000)")
                .isEqualByComparingTo(new BigDecimal("16500.00"));
        assertThat(summary.getNetBalance())
                .as("淨結餘應精準為 43,500.00")
                .isEqualByComparingTo(new BigDecimal("43500.00"));
    }

    @Test
    @DisplayName("TC-LEDGER-IT-02: 無記帳記錄月份零值防禦 (COALESCE 空指標安全防護)")
    void testMonthlySummaryZeroRecordBoundaryDefense() {
        User user = createAndPersistTestUser();

        MonthlySummaryVo summary = ledgerService.getMonthlySummary(user.getId(), 2020, 1);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalIncome())
                .as("無記錄時總收入應為 0 且無 NPE")
                .isNotNull()
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalExpense())
                .as("無記錄時總支出應為 0 且無 NPE")
                .isNotNull()
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getNetBalance())
                .as("無記錄時淨結餘應為 0 且無 NPE")
                .isNotNull()
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("TC-LEDGER-IT-03: 多維度 Specification 動態條件查詢 (複合條件與降序排序)")
    void testMultiDimensionalSpecificationDynamicQuery() {
        User user = createAndPersistTestUser();
        Category foodCategory = categoryRepository.findAvailableCategoriesByType(user.getId(), "EXPENSE")
                .stream().filter(c -> "飲食聚餐".equals(c.getName())).findFirst().orElseThrow();
        Category transportCategory = categoryRepository.findAvailableCategoriesByType(user.getId(), "EXPENSE")
                .stream().filter(c -> "交通出行".equals(c.getName())).findFirst().orElseThrow();

        // 記錄 1: 飲食, 2026-05-10, 關鍵字 "午餐便當"
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("150.00"))
                .description("平日美味午餐便當")
                .recordDate(LocalDate.of(2026, 5, 10))
                .build());

        // 記錄 2: 飲食, 2026-05-12, 關鍵字 "晚餐火鍋"
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("300.00"))
                .description("精選日式晚餐火鍋")
                .recordDate(LocalDate.of(2026, 5, 12))
                .build());

        // 記錄 3: 交通, 2026-05-15, 關鍵字 "高鐵車票" (分類不符)
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(transportCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("1200.00"))
                .description("商務差旅高鐵車票")
                .recordDate(LocalDate.of(2026, 5, 15))
                .build());

        // 記錄 4: 飲食, 2026-06-01, 關鍵字 "早餐咖啡" (日期區間不符)
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("200.00"))
                .description("晨間提神早餐咖啡")
                .recordDate(LocalDate.of(2026, 6, 1))
                .build());

        // 多維度複合查詢：userId + EXPENSE + foodCategory + 2026-05-01~2026-05-31 + 關鍵字 "餐"
        Page<RecordResponseVo> resultPage = ledgerService.queryRecords(
                user.getId(),
                "EXPENSE",
                foodCategory.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "餐",
                PageRequest.of(0, 10)
        );

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).as("複合篩選應精準命中 2 筆符合條件記錄").isEqualTo(2L);
        assertThat(resultPage.getContent()).hasSize(2);

        // 驗證預設日期倒序排序：2026-05-12 應在 2026-05-10 前面
        RecordResponseVo first = resultPage.getContent().get(0);
        RecordResponseVo second = resultPage.getContent().get(1);
        assertThat(first.getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(first.getDescription()).contains("晚餐火鍋");
        assertThat(first.getCategoryName()).isEqualTo("飲食聚餐");

        assertThat(second.getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(second.getDescription()).contains("午餐便當");
    }

    @Test
    @DisplayName("TC-LEDGER-IT-04: 記帳 CRUD 完整生命週期與資料落盤一致性")
    void testCrudLifecycleAndEntityStateConsistency() {
        User user = createAndPersistTestUser();
        Category foodCategory = categoryRepository.findAvailableCategoriesByType(user.getId(), "EXPENSE").get(0);

        // 1. Create
        RecordCreateRequestDto createDto = RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("250.00"))
                .description("午後香醇下午茶")
                .recordDate(LocalDate.of(2026, 5, 18))
                .build();
        RecordResponseVo created = ledgerService.createRecord(user.getId(), createDto);

        assertThat(created.getId()).isNotNull().isPositive();
        assertThat(created.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(created.getDescription()).isEqualTo("午後香醇下午茶");

        // 2. Read
        RecordResponseVo queried = ledgerService.getRecordById(created.getId(), user.getId());
        assertThat(queried.getId()).isEqualTo(created.getId());
        assertThat(queried.getDescription()).isEqualTo("午後香醇下午茶");

        // 3. Update
        RecordUpdateRequestDto updateDto = RecordUpdateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("350.00"))
                .description("午後香醇下午茶升級雙人套餐")
                .recordDate(LocalDate.of(2026, 5, 18))
                .build();
        RecordResponseVo updated = ledgerService.updateRecord(created.getId(), user.getId(), updateDto);
        assertThat(updated.getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(updated.getDescription()).isEqualTo("午後香醇下午茶升級雙人套餐");

        // 強制 Hibernate 刷盤並清理一級快取，確保真實資料庫一致性
        entityManager.flush();
        entityManager.clear();

        RecordResponseVo reloaded = ledgerService.getRecordById(created.getId(), user.getId());
        assertThat(reloaded.getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));

        // 4. Delete
        ledgerService.deleteRecord(created.getId(), user.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(accountRecordRepository.findById(created.getId())).isEmpty();
        assertThatThrownBy(() -> ledgerService.getRecordById(created.getId(), user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("TC-LEDGER-IT-05: 跨租戶橫向越權存取防禦 (IDOR 安全隔離檢驗)")
    void testCrossTenantHorizontalPrivilegeEscalationDefense() {
        User userA = createAndPersistTestUser();
        User userB = createAndPersistTestUser();
        Category foodCategory = categoryRepository.findAvailableCategoriesByType(userB.getId(), "EXPENSE").get(0);

        // 由 User B 建立一筆私密流水帳
        RecordResponseVo recordB = ledgerService.createRecord(userB.getId(), RecordCreateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("999.00"))
                .description("User B 的私密記帳")
                .recordDate(LocalDate.of(2026, 5, 20))
                .build());

        // User A 嘗試讀取 User B 的記帳記錄 -> 預期拋出 ResourceNotFoundException 拒絕
        assertThatThrownBy(() -> ledgerService.getRecordById(recordB.getId(), userA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // User A 嘗試修改 User B 的記帳記錄 -> 預期拋出 ResourceNotFoundException 拒絕
        RecordUpdateRequestDto maliciousUpdate = RecordUpdateRequestDto.builder()
                .categoryId(foodCategory.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("1.00"))
                .description("遭惡意竄改")
                .recordDate(LocalDate.of(2026, 5, 20))
                .build();
        assertThatThrownBy(() -> ledgerService.updateRecord(recordB.getId(), userA.getId(), maliciousUpdate))
                .isInstanceOf(ResourceNotFoundException.class);

        // User A 嘗試刪除 User B 的記帳記錄 -> 預期拋出 ResourceNotFoundException 拒絕
        assertThatThrownBy(() -> ledgerService.deleteRecord(recordB.getId(), userA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 驗證 User B 的記帳記錄完好無損未遭竄改
        RecordResponseVo intactRecord = ledgerService.getRecordById(recordB.getId(), userB.getId());
        assertThat(intactRecord.getAmount()).isEqualByComparingTo(new BigDecimal("999.00"));
        assertThat(intactRecord.getDescription()).isEqualTo("User B 的私密記帳");
    }

    @Test
    @DisplayName("TC-LEDGER-IT-06: 關聯不存在分類建立記帳防禦 (外鍵邊界安全保護)")
    void testCreateRecordWithNonExistentCategoryDefense() {
        User user = createAndPersistTestUser();

        RecordCreateRequestDto invalidDto = RecordCreateRequestDto.builder()
                .categoryId(999999L) // 不存在的分類 ID
                .recordType("EXPENSE")
                .amount(new BigDecimal("500.00"))
                .description("無效分類記帳")
                .recordDate(LocalDate.of(2026, 5, 20))
                .build();

        assertThatThrownBy(() -> ledgerService.createRecord(user.getId(), invalidDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");

        // 驗證未有任何髒資料寫入
        long userRecordCount = accountRecordRepository.count();
        assertThat(accountRecordRepository.findByIdAndUserId(1L, user.getId())).isEmpty();
    }
}
