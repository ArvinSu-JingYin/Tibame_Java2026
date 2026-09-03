# 每日記帳系統 (Daily Ledger System) - 核心業務服務單元測試設計與情境矩陣報告

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-02  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / JUnit 5 / Mockito / AssertJ  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **測試目標模組**：`CategoryService` (`CategoryServiceImpl`), `LedgerService` (`LedgerServiceImpl`)  

---

## 1. 測試架構與隔離設計 (Testing Architecture)

本專案的核心業務服務測試遵循 **純單元測試 (Pure Unit Testing)** 規範，旨在以毫秒級的執行速度，針對商業邏輯、多租戶隔離與異常控制流程進行全覆蓋驗證。

```
+-----------------------------------------------------------------------------------------+
|                                單元測試執行架構 (Unit Test Architecture)                 |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   [ CategoryServiceTest ]                       [ LedgerServiceTest ]                   |
|              |                                            |                             |
|              v                                            v                             |
|   +---------------------+                      +---------------------+                  |
|   | CategoryServiceImpl |                      |  LedgerServiceImpl  |                  |
|   +---------------------+                      +---------------------+                  |
|         |           |                                |          |          |            |
|       @Mock       @Mock                            @Mock      @Mock      @Mock          |
|         v           v                                v          v          v            |
|   [CategoryRepo] [RecordRepo]                  [RecordRepo] [CatRepo] [SmartParser]     |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 設計原則與標準

1. **極速執行與零環境依賴**：
   - 採用 `@ExtendWith(MockitoExtension.class)`。
   - 不啟動 Spring `ApplicationContext`，不依賴外部資料庫或網路連接。
2. **AAA (Arrange - Act - Assert) 模式**：
   - **Arrange**：準備測試假資料 (Request DTO / Entity)，配置 Mockito `when(...).thenReturn(...)`。
   - **Act**：調用待測方法 (Target Service Method)。
   - **Assert & Verify**：斷言回傳 VO 內容、斷言業務例外 (Exception)，並以 `verify(...)` 驗證相依 Repository 調用次數。
3. **清晰語意化命名**：
   - 每個測試案例皆標註 `@DisplayName("測試情境與預期結果繁體中文描述")`。

---

## 2. CategoryService 單元測試設計矩陣

`CategoryServiceImpl` 掌管收支分類的建立、查詢、修改與刪除，包含系統內建分類保護機制、同名衝突檢查與外鍵關聯刪除防護。

```
+-----------------------------------------------------------------------------------------+
| 方法名稱             | 測試情境 (Scenario)                     | 預期行為 / 斷言檢驗   |
+-----------------------------------------------------------------------------------------+
| getCategories       | 1. 不帶 type 查詢全部可用分類 (系統+個人)  | 回傳全量分類清單 VO    |
|                     | 2. 帶 type="EXPENSE" 篩選支出分類        | 調用依類型篩選 Repo   |
|---------------------+-----------------------------------------+------------------------|
| getCategoryById     | 3. 查詢存在且屬於當前使用者/系統的分類    | 回傳對應分類 VO        |
|                     | 4. 查詢不存在或無權存取之分類 ID          | 拋出 ResourceNotFound  |
|---------------------+-----------------------------------------+------------------------|
| createCategory      | 5. 建立自訂分類 (使用預設 icon/sortOrder)| 成功建立，預設值生效   |
|                     | 6. 建立自訂分類 (指定自訂 icon 與排序)   | 成功建立，保存指定欄位 |
|                     | 7. 建立已存在之同類型同名稱分類          | 拋出 ConflictException |
|---------------------+-----------------------------------------+------------------------|
| updateCategory      | 8. 正常更新自己的自訂分類                | 成功更新並回傳最新 VO  |
|                     | 9. 嘗試修改系統內建分類 (isSystem=true)  | 拋出 ForbiddenException|
|                     | 10. 嘗試修改他人建立之自訂分類 (跨租戶)   | 拋出 ForbiddenException|
|                     | 11. 更新不存在之分類 ID                  | 拋出 ResourceNotFound  |
|---------------------+-----------------------------------------+------------------------|
| deleteCategory      | 12. 正常刪除無記帳記錄引用之自訂分類     | 成功調用 delete 刪除   |
|                     | 13. 嘗試刪除已有記帳記錄引用之分類       | 拋出 ConflictException |
|                     | 14. 嘗試刪除系統內建分類                 | 拋出 ForbiddenException|
|                     | 15. 嘗試刪除他人建立之自訂分類 (跨租戶)   | 拋出 ForbiddenException|
|                     | 16. 刪除不存在之分類 ID                  | 拋出 ResourceNotFound  |
+-----------------------------------------------------------------------------------------+
```

### CategoryService 核心驗證細節

```
                   +----------------------------------+
                   |  CategoryService 安全與商務防禦  |
                   +----------------------------------+
                                     |
         +---------------------------+---------------------------+
         |                                                       |
         v                                                       v
  [ 系統內建分類唯讀保護 ]                                 [ 外鍵引用完整性防護 ]
  - isSystem == true                                     - countByCategoryId(id) > 0
  - 禁止 updateCategory                                  - 禁止 deleteCategory
  - 禁止 deleteCategory                                  - 避免產生孤兒記帳明細
```

---

## 3. LedgerService 單元測試設計矩陣

`LedgerServiceImpl` 掌管收支帳目 CRUD、收支類別一致性防禦、智慧文字快速記帳（代理調用 `SmartParserService`）、動態分頁查詢與月度財務結餘報表統計。

```
+-----------------------------------------------------------------------------------------+
| 方法名稱             | 測試情境 (Scenario)                     | 預期行為 / 斷言檢驗   |
+-----------------------------------------------------------------------------------------+
| createRecord        | 1. 正常建立支出記帳 (EXPENSE)           | 成功保存並回傳 Rich VO |
|                     | 2. 正常建立收入記帳 (INCOME)            | 成功保存並回傳 Rich VO |
|                     | 3. 指定不存在或無權存取的分類 ID         | 拋出 ResourceNotFound  |
|                     | 4. 記帳類型與所選分類類型不符 (型態衝突) | 拋出 ApiException      |
|---------------------+-----------------------------------------+------------------------|
| quickCreateRecord   | 5. 快速字串輸入 (代理 SmartParser 解析)  | 解析後自動建立記帳記錄 |
|---------------------+-----------------------------------------+------------------------|
| updateRecord        | 6. 正常更新記帳金額、分類、日期與備註   | 成功更新並保存         |
|                     | 7. 嘗試更新不存在或非本人之記帳記錄     | 拋出 ResourceNotFound  |
|                     | 8. 更新時變更為不合法的分類 ID           | 拋出 ResourceNotFound  |
|                     | 9. 更新時記帳類型與新分類類型不一致     | 拋出 ApiException      |
|---------------------+-----------------------------------------+------------------------|
| deleteRecord        | 10. 正常刪除自己的記帳記錄               | 成功調用 delete 刪除   |
|                     | 11. 嘗試刪除不存在或非本人之記帳記錄     | 拋出 ResourceNotFound  |
|---------------------+-----------------------------------------+------------------------|
| getRecordById       | 12. 查詢單筆記帳 (含關聯分類資訊富化)   | 回傳帶分類名稱/Icon VO |
|                     | 13. 查詢單筆記帳 (分類為 null 孤兒記錄)  | 名稱 fallback "未分類" |
|                     | 14. 查詢不存在或非本人之記帳記錄         | 拋出 ResourceNotFound  |
|---------------------+-----------------------------------------+------------------------|
| queryRecords        | 15. 多條件 Specification 動態查詢分頁    | 回傳 Page<VO>，批次富化|
|---------------------+-----------------------------------------+------------------------|
| getMonthlySummary   | 16. 正常年月統計 (收入、支出、淨結餘計算)| 驗證 netBalance 計算   |
|                     | 17. 當月無任何收支 (DB 回傳 null 金額)   | 自動轉為 0，安全無 NPE |
|                     | 18. 月初與月末日期邊界計算 (含閏月)      | 驗證 atDay(1) 與月末日 |
+-----------------------------------------------------------------------------------------+
```

### LedgerService 核心驗證細節

```
                     +------------------------------------+
                     |    LedgerService 業務邏輯防線      |
                     +------------------------------------+
                                       |
             +-------------------------+-------------------------+
             |                                                   |
             v                                                   v
   [ 收支類型強制一致性檢驗 ]                               [ 財務報表結餘精確度 ]
   - category.type == record.type                         - totalIncome = sum(INCOME) ?: 0
   - 防範「支出分類」記錄為「收入帳目」                    - totalExpense = sum(EXPENSE) ?: 0
   - 違規即拋出 ApiException                              - netBalance = Income - Expense
```

---

## 4. 多租戶資料隔離 (Multi-Tenant Isolation) 防護清單

在所有單元測試中，必須嚴格驗證跨租戶（跨使用者）的安全性邊界：

1. **水平越權防護 (IDOR Protection)**：
   - 查詢、修改、刪除操作必須綁定 `userId`。
   - 使用者 A 無法讀取、修改或刪除使用者 B 的記帳記錄與自訂分類。
2. **系統資源防禦**：
   - 系統共用分類 (`isSystem = true`) 允許所有使用者讀取與引用。
   - 任何使用者皆不可修改或刪除系統共用分類。
3. **孤兒記錄相容性**：
   - 即使關聯分類遺失或被清空，記帳記錄查詢時需維持健壯性，提供預設值（「未分類」/ 預設標籤圖示），不得拋出 `NullPointerException`。

---

## 5. 測試程式碼實作參考範例 (AAA Pattern)

### 範例 1：`CategoryServiceTest.java` 衝突防護測試

```java
@Test
@DisplayName("測試建立重複名稱自訂分類應拋出 ConflictException")
void testCreateCategoryDuplicateThrowsConflict() {
    // Arrange
    Long userId = 1L;
    CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
            .name("餐飲美食")
            .type("EXPENSE")
            .build();

    when(categoryRepository.existsByUserIdAndTypeAndName(userId, "EXPENSE", "餐飲美食"))
            .thenReturn(true);

    // Act & Assert
    assertThrows(ConflictException.class, () -> categoryService.createCategory(userId, request));
    verify(categoryRepository, never()).save(any(Category.class));
}
```

### 範例 2：`LedgerServiceTest.java` 月度收支與結餘統計測試

```java
@Test
@DisplayName("測試月度收支統計計算與淨結餘 (含無收支 Null 安全處理)")
void testGetMonthlySummaryWithCalculations() {
    // Arrange
    Long userId = 1L;
    int year = 2026;
    int month = 8;
    LocalDate startDate = LocalDate.of(2026, 8, 1);
    LocalDate endDate = LocalDate.of(2026, 8, 31);

    when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
            userId, "INCOME", startDate, endDate)).thenReturn(new BigDecimal("60000"));
    when(accountRecordRepository.sumAmountByUserIdAndRecordTypeAndDateRange(
            userId, "EXPENSE", startDate, endDate)).thenReturn(new BigDecimal("25000"));

    // Act
    MonthlySummaryVo summary = ledgerService.getMonthlySummary(userId, year, month);

    // Assert
    assertNotNull(summary);
    assertEquals(year, summary.getYear());
    assertEquals(month, summary.getMonth());
    assertEquals(new BigDecimal("60000"), summary.getTotalIncome());
    assertEquals(new BigDecimal("25000"), summary.getTotalExpense());
    assertEquals(new BigDecimal("35000"), summary.getNetBalance());
}
```

---

## 6. 後續實施路線 (Next Steps)

本文件確立了 `CategoryService` 與 `LedgerService` 的單元測試規格。後續可依循此情境矩陣進行實作：
- 建立 `src/test/java/com/tibame/service/CategoryServiceTest.java`（涵蓋 16 個測試案例）。
- 建立 `src/test/java/com/tibame/service/LedgerServiceTest.java`（涵蓋 18 個測試案例）。
- 實施後執行 `./mvnw test`，預計將專案測試案例總數由 20 個提升至 54 個，核心業務層覆蓋率達 90% 以上。
