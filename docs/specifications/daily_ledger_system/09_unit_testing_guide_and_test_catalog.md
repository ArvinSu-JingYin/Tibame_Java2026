# 9. 單元測試操作手冊與測試案例盤點清單 (Unit Testing Guide & Test Catalog)

> **專案代號**：`daily-ledger-system`  
> **所屬模組**：單元測試操作手冊、AAA 撰寫範式、54 測試情境矩陣與開發檢核標準  
> **導覽指引**：[← 返回流水帳規格目錄 (README.md)](README.md) ｜ [下一篇：10 E2E 測試手冊 →](10_e2e_testing_guide_and_operation_manual.md) ｜ [← 返回專案總門戶 (docs/README.md)](../../README.md)  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / JUnit 5 (Jupiter) / Mockito / AssertJ / Maven Wrapper  
> **測試統計**：8 個測試類別，共 54 個單元測試案例（全數通過，綠燈率 100%，耗時約 2~5 秒）  
> **文件狀態**：正式發布 (`Active`)  
> **目標讀者**：後端開發者、架構師、測試工程師與代碼審查者 (Code Reviewer)  

---

## 1. 執行摘要與設計哲學 (Executive Summary & Philosophy)

本專案遵循 **純單元測試 (Pure Unit Testing)** 規範，旨在以毫秒級的極速反饋，保障業務核心、安全性、多租戶隔離與資料一致性。

```
+-----------------------------------------------------------------------------+
|                          純單元測試架構與隔離理念                           |
+-----------------------------------------------------------------------------+
|                                                                             |
|   [ 業務服務層測試 (Service Tests) ]       [ 核心密碼/工具測試 (Crypto/POJO) ] |
|   - CategoryServiceTest (16 個情境)        - CryptoServiceTest (AES-256-GCM)|
|   - LedgerServiceTest (18 個情境)          - PasswordServiceTest (BCrypt)   |
|   - AuthServiceTest (3 個情境)             - PasswordPolicyValidatorTest    |
|                                            - TokenServiceTest (JWT)         |
|              |                             - SmartParserServiceTest         |
|              v                                            |                 |
|    @ExtendWith(MockitoExtension)                          v                 |
|              |                                   純 Java 物件 (POJO)        |
|    +--------------------+                        零 Spring 依賴直接實例化    |
|    |   @InjectMocks     |                                                   |
|    |  待測 ServiceImpl  |                                                   |
|    +--------------------+                                                   |
|        |            |                                                       |
|      @Mock        @Mock                                                     |
|        v            v                                                       |
|  [CategoryRepo] [RecordRepo]                                                |
|  (記憶體隔離模擬，零 DB 連線開銷)                                            |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### 核心設計原則
1. **極速反饋 (Instant Feedback)**：完全不啟動 Spring `ApplicationContext`，不依賴外部資料庫或網路 I/O，54 個測試可在 5 秒內編譯並全部執行完畢。
2. **完全隔離 (Hermetic Isolation)**：受測單元與外部依賴（JPA Repository、外部 API）透過 Mockito 完全隔離，測試結果具備 100% 決定性 (Deterministic)，不受環境資料波動干擾。
3. **語意化繁體中文規範**：每個測試案例皆標註清晰的繁體中文 `@DisplayName("測試 [情境/操作] 應 [預期結果]")`，使測試程式碼同時作為活規格說明書 (Living Documentation)。

---

## 2. 單元測試操作指南 (Developer Usage Guide)

專案根目錄已內建 Maven Wrapper (`mvnw.cmd` / `mvnw`)，無需全域安裝 Maven 即可隨開即測。

### 2.1 命令列指令速查 (CLI Cheat Sheet)

```powershell
# 1. 執行全專案 54 個單元測試
.\mvnw.cmd test

# 2. 僅執行特定測試類別 (例如：LedgerServiceTest)
.\mvnw.cmd test -Dtest=LedgerServiceTest

# 3. 僅執行特定測試方法 (語法：類別名#方法名)
.\mvnw.cmd test -Dtest=CategoryServiceTest#testCreateCategoryDuplicateThrowsConflict

# 4. 僅執行特定套件下的所有測試 (例如：service 套件)
.\mvnw.cmd test -Dtest="com.tibame.service.*"

# 5. 同時執行多個指定測試類別 (以逗號分隔)
.\mvnw.cmd test -Dtest=CategoryServiceTest,LedgerServiceTest,AuthServiceTest

# 6. 啟用詳細除錯日誌輸出 (Debug Log)
.\mvnw.cmd test -X
```

### 2.2 IDE (IntelliJ IDEA / VS Code / Antigravity IDE) 操作
* **單鍵執行**：在任何 `*Test.java` 類別或方法宣告左側，點選綠色播放鍵 (`Run Test`)。
* **中斷點除錯 (Debug Test)**：若業務運算或資料轉換未達預期，可在業務 Service 實作中設定中斷點 (Breakpoint)，以 Debug 模式運行測試逐步跟蹤變數。
* **失敗測試重跑 (Rerun Failed Tests)**：重構期間點選測試視窗的「Rerun Failed Tests」，只重新驗證先前紅燈的案例，大幅加速開發週期。

---

## 3. 單元測試撰寫規範與 AAA 範式 (Coding Standards & AAA Pattern)

所有業務層單元測試必須嚴格實施 **AAA (Arrange-Act-Assert)** 模式：

```
+-----------------------------------------------------------------------------+
|                           AAA 測試生命週期步驟                              |
+-----------------------------------------------------------------------------+
|                                                                             |
|  1. [ Arrange ] (安排)  --> 準備假資料 (DTO/Entity)、設定 Mockito 預期回傳   |
|                                when(repo.findById(...)).thenReturn(...)     |
|                                                                             |
|  2. [ Act ]     (執行)  --> 呼叫受測服務方法                                 |
|                                service.updateCategory(userId, dto)          |
|                                                                             |
|  3. [ Assert ]  (驗證)  --> 斷言狀態值、捕獲異常、驗證 Repository 交互次數  |
|                                assertEquals / assertThrows / verify(...)    |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### 3.1 最佳實踐範例代碼

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 單元測試範例")
class CategoryServiceExampleTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("測試建立重複名稱自訂分類應拋出 ConflictException")
    void testCreateCategoryDuplicateThrowsConflict() {
        // 1. Arrange: 準備輸入 DTO 並設定 Mock 預期
        Long userId = 1L;
        CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                .name("餐飲美食")
                .type("EXPENSE")
                .build();
        when(categoryRepository.existsByUserIdAndTypeAndName(userId, "EXPENSE", "餐飲美食"))
                .thenReturn(true);

        // 2. Act & 3. Assert: 驗證拋出 ConflictException，且資料庫絕不寫入
        assertThrows(ConflictException.class, () -> categoryService.createCategory(userId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
```

---

## 4. 全專案 54 個單元測試盤點目錄 (Complete Test Catalog)

### 4.1 核心業務服務層 (`com.tibame.service`) - 共 37 個測試

#### A. `CategoryServiceTest` (16 個測試) - 分類管理業務邏輯
原始碼位置：`src/test/java/com/tibame/service/CategoryServiceTest.java`

| # | 測試方法名稱 | 測試情境與業務防禦重點 | 斷言 / 驗證目標 |
|---|---|---|---|
| 1 | `testGetCategoriesWithoutType_ReturnsAllAvailableCategories` | 查詢全部可用分類（包含系統內建與個人自訂） | 回傳所有分類 VO，清單數量與內容正確 |
| 2 | `testGetCategoriesWithTypeExpense_ReturnsOnlyExpenseCategories` | 帶 `type="EXPENSE"` 參數篩選分類 | 僅調用指定型態之查詢方法並回傳支出分類 |
| 3 | `testGetCategoryById_WhenExistsAndOwnedByUser_ReturnsCategoryVo` | 正常查詢自己建立的自訂分類 | 成功回傳對應分類 VO，各欄位值相符 |
| 4 | `testGetCategoryById_WhenSystemCategory_ReturnsCategoryVo` | 正常查詢系統內建分類 (`isSystem=true`) | 跨租戶共用存取成功，回傳分類 VO |
| 5 | `testGetCategoryById_WhenNotExists_ThrowsResourceNotFoundException` | 查詢不存在之分類 ID | 拋出 `ResourceNotFoundException` (404) |
| 6 | `testGetCategoryById_WhenOwnedByOtherUser_ThrowsResourceNotFoundException` | **多租戶隔離**：嘗試查詢他人自訂分類 | 視為不存在，拋出 `ResourceNotFoundException` (404) |
| 7 | `testCreateCategory_WithDefaultValues_Success` | 建立自訂分類（缺省 icon/sortOrder 參數） | 成功保存，驗證預設圖示與預設排序正確套用 |
| 8 | `testCreateCategory_WithCustomValues_Success` | 建立自訂分類（指定自訂 icon 與排序） | 成功保存，驗證指定欄位正確寫入實體 |
| 9 | `testCreateCategory_DuplicateNameAndType_ThrowsConflictException` | **唯一性防禦**：建立同類型同名自訂分類 | 拋出 `ConflictException` (409)，永不儲存 |
| 10 | `testUpdateCategory_Success` | 正常修改自己的自訂分類名稱、圖示與排序 | 成功保存更新並回傳最新分類 VO |
| 11 | `testUpdateCategory_SystemCategory_ThrowsForbiddenException` | **系統資源保護**：嘗試修改系統內建分類 | 拋出 `ForbiddenException` (403)，嚴禁修改 |
| 12 | `testUpdateCategory_OwnedByOtherUser_ThrowsForbiddenException` | **水平越權防護 (IDOR)**：嘗試修改他人分類 | 拋出 `ForbiddenException` (403)，安全阻擋 |
| 13 | `testUpdateCategory_NotExists_ThrowsResourceNotFoundException` | 更新不存在之分類 ID | 拋出 `ResourceNotFoundException` (404) |
| 14 | `testDeleteCategory_Success` | 正常刪除自己無記帳記錄引用的自訂分類 | 成功調用 `delete` 刪除該分類 |
| 15 | `testDeleteCategory_HasRecords_ThrowsConflictException` | **外鍵關聯完整性**：分類已有記帳記錄引用 | 拋出 `ConflictException` (409)，防範孤兒明細 |
| 16 | `testDeleteCategory_SystemCategory_ThrowsForbiddenException` | **系統資源保護**：嘗試刪除系統內建分類 | 拋出 `ForbiddenException` (403)，嚴禁刪除 |

---

#### B. `LedgerServiceTest` (18 個測試) - 記帳收支核心業務邏輯
原始碼位置：`src/test/java/com/tibame/service/LedgerServiceTest.java`

| # | 測試方法名稱 | 測試情境與業務防禦重點 | 斷言 / 驗證目標 |
|---|---|---|---|
| 1 | `testCreateRecord_Expense_Success` | 正常建立支出記帳 (EXPENSE) | 關聯分類資訊富化、寫入 DB 並回傳完整 VO |
| 2 | `testCreateRecord_Income_Success` | 正常建立收入記帳 (INCOME) | 關聯分類資訊富化、寫入 DB 並回傳完整 VO |
| 3 | `testCreateRecord_CategoryNotFound_ThrowsResourceNotFoundException` | 記帳指定不存在或非本人的分類 ID | 拋出 `ResourceNotFoundException` (404) |
| 4 | `testCreateRecord_CategoryTypeMismatch_ThrowsApiException` | **型態防呆**：支出記帳綁定收入分類（或反之） | 拋出業務例外 `ApiException`，拒絕寫入 |
| 5 | `testQuickCreateRecord_Success` | 快速字串自然語言記帳（代理調用 SmartParser） | 成功解析金額、類型與分類並自動建立記帳記錄 |
| 6 | `testUpdateRecord_Success` | 正常更新記帳金額、分類、日期與備註 | 成功更新記錄，保存最新欄位資料 |
| 7 | `testUpdateRecord_RecordNotFoundOrNotOwned_ThrowsResourceNotFoundException` | **水平越權防護**：嘗試修改他人記帳記錄 | 拋出 `ResourceNotFoundException` (404) |
| 8 | `testUpdateRecord_NewCategoryNotFound_ThrowsResourceNotFoundException` | 更新記帳時變更為不合法或非本人的分類 ID | 拋出 `ResourceNotFoundException` (404) |
| 9 | `testUpdateRecord_NewCategoryTypeMismatch_ThrowsApiException` | 更新記帳時新分類型態與記帳類型衝突 | 拋出業務例外 `ApiException` |
| 10 | `testDeleteRecord_Success` | 正常刪除本人的記帳記錄 | 成功調用 `delete` 刪除該記錄 |
| 11 | `testDeleteRecord_RecordNotFoundOrNotOwned_ThrowsResourceNotFoundException` | **水平越權防護**：嘗試刪除他人記帳記錄 | 拋出 `ResourceNotFoundException` (404) |
| 12 | `testGetRecordById_SuccessWithCategoryInfo` | 單筆記帳查詢成功，並關聯分類資訊富化 | VO 正確攜帶分類名稱、圖示等資訊 |
| 13 | `testGetRecordById_WhenCategoryIsNull_ReturnsFallbackUncategorized` | **孤兒記錄相容性**：分類為 null 的記帳明細 | 分類名稱安全 fallback「未分類」，避免 NPE |
| 14 | `testGetRecordById_RecordNotFoundOrNotOwned_ThrowsResourceNotFoundException` | 查詢他人或不存在之記帳記錄 | 拋出 `ResourceNotFoundException` (404) |
| 15 | `testQueryRecords_WithDynamicSpecification_ReturnsPagedVoList` | 多條件 Specification 動態篩選分頁查詢 | 正確解析分頁參數，並批次富化分類快取資訊 |
| 16 | `testGetMonthlySummary_CalculatesNetBalanceAccurately` | 月度財務統計（總收、總支、淨結餘計算） | 淨結餘 = 總收入 - 總支出，金額精確無誤差 |
| 17 | `testGetMonthlySummary_WhenNoRecords_ReturnsZeroSafeWithoutNpe` | **Null 安全處理**：當月無收支 (DB 回傳 null 金額) | 自動轉換為 `BigDecimal.ZERO`，避免 NPE |
| 18 | `testGetMonthlySummary_DateRangeBoundaryCheck` | 月度統計邊界日期計算（含閏年、大小月） | 第一天正確對齊 `atDay(1)`，最後一天精確到月底 |

---

#### C. `AuthServiceTest` (3 個測試) - 認證與授權業務邏輯
原始碼位置：`src/test/java/com/tibame/service/AuthServiceTest.java`

| # | 測試方法名稱 | 測試情境與業務防禦重點 | 斷言 / 驗證目標 |
|---|---|---|---|
| 1 | `testRegisterSuccess` | 使用者正常註冊流程 | 驗證密碼強度、執行 BCrypt 雜湊並保存新使用者 |
| 2 | `testLoginSuccessWithAutoUpgrade` | 登入成功發行 JWT，並觸發無感密碼升級 | 偵測舊 Cost Factor 自動重新雜湊升級 (Auto-Upgrade) |
| 3 | `testLoginFailureOnWrongPassword` | 密碼不正確登入流程 | 拋出 `UnauthorizedException` (401)，絕不發行 Token |

---

### 4.2 密碼學與資安防禦模組 (`com.tibame.common.crypto`) - 共 13 個測試

#### D. `CryptoServiceTest` (5 個測試) - AES-256-GCM 模組化加解密
原始碼位置：`src/test/java/com/tibame/common/crypto/cipher/CryptoServiceTest.java`
* `testEncryptAndDecryptSuccess`：驗證正常加解密還原明文（包含繁體中文、特殊符號與長字串）。
* `testRandomIvProducesDifferentCiphertexts`：**語意安全**，相同明文每次加密必須採用獨立隨機 IV，產生不同密文字串。
* `testTamperedCiphertextThrowsCryptoException`：**AEAD 防竄改驗證**，當密文被竄改 1 bit 時，Auth Tag 檢驗失敗並拋出 `CryptoException`。
* `testInvalidEnvelopeFormatThrowsCryptoException`：格式不符之信封標頭驗證防呆。
* `testNullOrEmptyInputThrowsException`：空值或空白字串安全防呆。

#### E. `PasswordServiceTest` (4 個測試) - BCrypt 密碼雜湊運算
原始碼位置：`src/test/java/com/tibame/common/crypto/password/PasswordServiceTest.java`
* `testHashAndVerifySuccess`：密碼雜湊與比對驗證。
* `testDifferentSaltsProduceDifferentHashes`：每次雜湊自動注入隨機 Salt。
* `testNeedsUpgradeWhenCostFactorLower`：當現存 Hash 的 Cost Factor 小於系統設定時，精確觸發升級需求。
* `testNullOrEmptyPasswordThrowsException`：空密碼輸入例外防呆。

#### F. `PasswordPolicyValidatorTest` (4 個測試) - 密碼強度規則防線
原始碼位置：`src/test/java/com/tibame/common/crypto/password/PasswordPolicyValidatorTest.java`
* `testValidPasswordPasses`：符合複雜度要求（含大小寫、數字、特殊符號）通過檢驗。
* `testPasswordTooShortThrowsException`：長度低於最小限制時拒絕註冊。
* `testPasswordMissingSpecialCharThrowsException`：缺少特殊符號時拒絕註冊。
* `testPasswordBlankThrowsException`：空白字串拒絕註冊。

---

### 4.3 智慧文字解析與 Token 服務 (`com.tibame`) - 共 4 個測試

#### G. `TokenServiceTest` (2 個測試) & `SmartParserServiceTest` (2 個測試)
原始碼位置：`src/test/java/com/tibame/TokenServiceTest.java` & `src/test/java/com/tibame/SmartParserServiceTest.java`
* `testGenerateAndValidateToken`：JWT 簽發、Claims 提取與有效性驗證。
* `testExpiredOrInvalidToken`：偽造簽名或過期 JWT 安全拒絕。
* `testParseExpenseString`：智慧自然語言記帳字串解析（如「午餐便當 120 元」提取金額與品名）。
* `testParseIncomeString`：智慧自然語言收入字串解析。

---

## 5. 核心業務安全防禦矩陣 (Security & Business Defense Matrix)

在目前的測試體系中，特別建立以下四道不可妥協的業務安全防線：

```
                  +--------------------------------+
                  |    核心業務單元測試防線矩陣    |
                  +--------------------------------+
                                   |
         +-------------------------+-------------------------+
         |                         |                         |
         v                         v                         v
  [ 多租戶水平越權防護 ]    [ 系統資源唯讀保護 ]      [ 業務資料一致性與防呆 ]
  - 查詢他人記錄 -> 404     - isSystem == true 分類   - 記帳類型與分類類型衝突
  - 修改他人自訂分類 -> 403 - 嚴禁 UPDATE (403)       - 分類已有關聯記帳禁止刪除
  - 刪除他人記帳記錄 -> 404 - 嚴禁 DELETE (403)       - 月度收支 Null 金額補 0
```

---

## 6. 後續新功能開發測試檢核清單 (Developer Checklist)

未來開發新 Service 或擴充業務功能時，請團隊務必對照以下清單進行自我審查：

```
+-----------------------------------------------------------------------------+
|                         新功能單元測試檢核清單                              |
+-----------------------------------------------------------------------------+
| [ ] 1. 隔離性原則: 使用 @ExtendWith(MockitoExtension.class)，絕不啟動 Spring |
| [ ] 2. 中文命名規範: 標註繁體中文 @DisplayName("測試 [行為] 應 [預期結果]")  |
| [ ] 3. 正常路徑 (Happy Path): 驗證正常輸入能成功持久化並回傳正確 VO         |
| [ ] 4. 異常防禦 (Unhappy Path): 驗證非法參數或衝突時拋出對應業務 Exception    |
| [ ] 5. 多租戶隔離防衛: 驗證 userId 不匹配時拋出 403 Forbidden 或 404        |
| [ ] 6. 交互次數確認: 驗證未授權或失敗時使用 verify(repo, never()).save(...)  |
| [ ] 7. 數值與空值防禦: 金額必須使用 BigDecimal，並處理 DB 回傳 null 之情境  |
| [ ] 8. 本地全量回歸: 送出 Commit 前在本機執行 .\mvnw.cmd test 確保全數綠燈   |
+-----------------------------------------------------------------------------+
```
