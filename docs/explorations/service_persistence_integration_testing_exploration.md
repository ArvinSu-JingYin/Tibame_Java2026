# 每日流水帳系統 (Daily Ledger System) — 服務層持久化整合測試架構設計與邊界驗證探索報告

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-05  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Java 21 / Spring Boot 3.3.13 / Spring Data JPA / Hibernate / JUnit 5 / H2 In-Memory (MODE=MSSQLServer) / Microsoft SQL Server 2022  
> **目標範疇**：服務層業務邏輯與 JPA 持久層真實聯動、JPQL 聚合計算精準度、動態 Specification 多維度查詢、外鍵級聯防禦、跨租戶資料隔離與事務回滾機制  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md) ｜ [自動化測試架構篇](automated_testing_strategy_and_exploration.md) ｜ [E2E 測試探索報告 v2.0](e2e_testing_guide_and_operation_manual_exploration_v2.0.md) ｜ [整合測試範疇與實施策略 v3.0](integration_testing_scope_and_strategy_exploration_v3.0.md)

---

## 1. 探索背景與現有測試盲區診斷 (Background & Gap Analysis)

本專案「每日流水帳系統（Daily Ledger System）」在工程品質上已建置了單元測試（Surefire）與端到端測試（Failsafe Playwright + RANDOM_PORT API E2E）。然而，在目前的測試金字塔架構中，存在著顯著的**「中間斷層」**：

```
+---------------------------------------------------------------------------------------------------+
|                                專案測試架構現狀與中層斷層診斷                                     |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   [頂層 UI 真機 E2E]       Playwright Chromium (AccountingFlowUiE2ETest, AuthFlowUiE2ETest)       |
|                            - 優點: 貼近使用者真機視覺操作                                         |
|                            - 局限: 執行耗時較長 (~數秒至數十秒)，排查問題鏈路深                   |
|                                     ^                                                             |
|   [全端 API 整合測試]      TestRestTemplate + RANDOM_PORT (LedgerApiE2ETest 等)                   |
|                            - 局限: 獨立 HTTP 執行緒，無法套用 @Transactional 自動回滾，需手動清理 |
|                                     ^                                                             |
|                                     |   【重大斷層盲區 (The Missing Middle Layer)】               |
|                                     |   1. 缺乏 Service + Real JPA 真正落盤驗證                   |
|                                     |   2. 缺乏 JPQL 聚合函數 (COALESCE/SUM) 與動態 SQL 語法校驗  |
|                                     |   3. 缺乏資料庫外鍵約束 (FK Constraint) 刪除防護檢驗        |
|                                     |   4. 缺乏真正的資料庫事務自動回滾 (Zero Dirty Data)         |
|                                     v                                                             |
|   [底層純單元測試]         JUnit 5 + Mockito (54 個案例，毫秒級)                                  |
|                            - 優點: 速度極快                                                       |
|                            - 致命局限: Repository 全部由 Mockito 模擬 (when().thenReturn())，     |
|                              完全無法發現 SQL 語法錯誤、型別轉換失敗與資料庫約束衝突              |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### 1.1 現狀核心痛點
1. **單元測試 Mockito 過度虛擬化**：
   現有的 `LedgerServiceTest` 與 `CategoryServiceTest` 大量依賴 `Mockito.when(accountRecordRepository.findById(...))`。這種純記憶體隔離測試無法驗證：
   - Spring Data JPA 動態 Specification 生成的真實 SQL 是否合法。
   - 自訂 JPQL 查詢語法（例如：`SELECT COALESCE(SUM(r.amount), 0) FROM AccountRecord r WHERE ...`）在不同資料庫引擎上的表現。
   - 外鍵關聯與實體級聯設定是否會產生 `DataIntegrityViolationException`。
2. **全端 API E2E 難以進行細粒度狀態邊界驗證**：
   全端 API 測試成本高昂且必須啟動 Web Container，無法精準對 Service 的內部交易邊界與私有邏輯進行快速、隔離且無污染的深層檢驗。
3. **基礎建設已備，亟待業務領域實裝**：
   專案已具備 `IntegrationTestBase.java`、`application-test.yml`（H2 MSSQL 模式）與 `application-test-mssql.yml`（SQL Server 實體庫），但目前僅有 `DatabaseIntegrationIT` 驗證了種子資料載入，尚未將業務核心邏輯納入整合測試網。

---

## 2. 服務層持久化整合測試架構設計 (Architecture Design)

服務層持久化整合測試的核心目標：**在無 Web 容器負擔下，直接驅動 Spring IoC 容器中的真實 Service 與 Repository，在真實交易邊界內對真實資料庫進行增刪改查與複雜查詢驗證，並在測試結束後由 `@Transactional` 自動回滾，達成「真實持久化、極速反饋、零髒資料」三大目標**。

```
+---------------------------------------------------------------------------------------------------+
|                        服務層持久化整合測試 (Service Persistence IT) 結構                         |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   [測試啟動環境]              @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE) |
|   [資料庫交易控制]            @Transactional (Class-level 宣告，測試案例結束 100% 自動 ROLLBACK)   |
|   [活躍配置環境]              @ActiveProfiles("test") (支援平滑切換 -Dspring.profiles.active=test-mssql)
|                                     |                                                             |
|                                     v                                                             |
|   [被測目標 (SUT)]            LedgerService / CategoryService / AuthService                       |
|                               (真實 Spring 服務實例，注入真實 JPA Repository)                     |
|                                     |                                                             |
|                                     v                                                             |
|   [持久層 (JPA Layer)]        AccountRecordRepository / CategoryRepository / UserRepository        |
|                               - 執行真實 SQL 查詢與 JPQL 聚合運算                                 |
|                               - 執行 JPA Specification 動態多條件拼接                             |
|                               - 驗證實體關係對應與審計欄位 (createdAt, updatedAt)                 |
|                                     |                                                             |
|                                     v                                                             |
|   [底層資料庫引擎]            H2 In-Memory DB (MODE=MSSQLServer) ──[可切換]──> 本機 MS SQL Server |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. 測試體系對比與職責邊界 (Responsibility Boundary)

為避免重複測試或定位模糊，測試各層級之責任邊界明確劃分如下：

| 特性維度 | 純單元測試 (Unit Test) | 服務層持久化整合測試 (Service IT) | 全鏈路 API E2E (API E2E) | 瀏覽器 UI E2E (Playwright) |
| :--- | :--- | :--- | :--- | :--- |
| **主要定位** | 單一類別/純演算法計算 | 業務服務 + 真實資料庫持久化 + 事務 | HTTP 網路鏈路 + 安全 Filter | 使用者真實瀏覽器互動 |
| **被測目標** | Service (Mock Repo) | Service (Real Repo) | Controller + Spring Web | 前端 HTML/JS/CSS + 後端 |
| **Web 伺服器** | ❌ 無 | ❌ 無 (webEnvironment=NONE) | ⭕ 啟動 (RANDOM_PORT) | ⭕ 啟動 (RANDOM_PORT) |
| **資料庫連線** | ❌ 無 | ⭕ 真實連線 (H2 / MSSQL) | ⭕ 真實連線 (H2) | ⭕ 真實連線 (H2) |
| **交易自動回滾** | ➖ 不適用 | ✅ `@Transactional` 100% 回滾 | ❌ 無法回滾 (獨立 HTTP 緒) | ❌ 無法回滾 |
| **執行耗時** | 毫秒級 (< 50ms) | 極快 (~ 100ms - 250ms) | 中等 (~ 1s - 2s) | 較慢 (~ 3s - 10s) |
| **Maven 生命週期** | `mvn test` (Surefire) | `mvn verify` (Failsafe, `*IT.java`) | `mvn verify` (Failsafe) | `mvn verify` (Failsafe) |

---

## 4. 三大核心業務整合測試類別矩陣 (Test Catalog Matrix)

### 4.1 抽象基底類別：`ServiceIntegrationTestBase.java`
統一提供測試環境宣告、常用 Repository 注入與便利的測試資料製造工廠：

```java
package com.tibame.integration.base;

import com.tibame.common.crypto.password.PasswordService;
import com.tibame.model.entity.User;
import com.tibame.repository.AccountRecordRepository;
import com.tibame.repository.CategoryRepository;
import com.tibame.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 服務層持久化整合測試基底抽象類別
 * <p>
 * 1. 關閉 Web 伺服器啟動 (webEnvironment = NONE)，保持極速反饋。<br>
 * 2. 類別層級宣告 @Transactional，保證所有測試方法異動 100% 自動回滾，資料零污染。<br>
 * 3. 預設套用 test 設定檔 (H2 記憶體資料庫)，可平滑切換 test-mssql 實體驗收。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@ActiveProfiles("test")
public abstract class ServiceIntegrationTestBase {

    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected AccountRecordRepository accountRecordRepository;
    @Autowired protected PasswordService passwordService;

    /**
     * 快速建立並持久化一個隨機測試使用者（動態 UUID 杜絕唯一鍵衝突）
     */
    protected User createAndPersistTestUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .username("it_user_" + suffix)
                .email("it_user_" + suffix + "@tibame.com")
                .passwordHash(passwordService.hashPassword("Password123!"))
                .displayName("整合測試專用用戶")
                .build();
        return userRepository.save(user);
    }
}
```

---

### 4.2 類別一：`LedgerServicePersistenceIT.java`（流水帳業務持久化與查詢整合）

涵蓋記帳業務的核心計算、動態篩選與多租戶防禦：

| 案例編號 | 測試案例名稱 | 驗證情境與輸入數據 | 預期結果與核心斷言 | 防禦與技術價值 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-LEDGER-IT-01** | 月度統計聚合運算精準度 | 建立 2 筆收入 (50,000, 10,000)、3 筆支出 (12,000, 3,000, 1,500)、1 筆次月支出 (5,000) | 呼叫 `getMonthlySummary`：總收入 60,000、總支出 16,500、淨結餘 43,500；次月資料正確排除 | 驗證真實 JPQL `COALESCE(SUM(r.amount), 0)` 與日期區間過濾 |
| **TC-LEDGER-IT-02** | 當月無記錄時統計零值防禦 | 查詢完全無流水帳記錄的月份 (如 2020 年 1 月) | 總收入、總支出與淨結餘均為 `BigDecimal.ZERO`，無 NPE | 驗證 SQL `COALESCE` 函式防止資料庫回傳 NULL 導致的空指標 |
| **TC-LEDGER-IT-03** | 多維度 Specification 動態查詢 | 寫入多筆不同分類、不同日期、不同類型與關鍵字之記錄；以「分類 + 日期區間 + 關鍵字」複合查詢 | 分頁查詢回傳筆數精準吻合，生成的 SQL 具備完整的 AND 條件與排序 | 驗證 Spring Data JPA `JpaSpecificationExecutor` 動態 SQL 語法 |
| **TC-LEDGER-IT-04** | 完整 CRUD 生命週期與資料一致性 | 建立記帳 -> 查詢驗證落盤 -> 更新金額與分類 -> 重新讀取比對 -> 刪除記錄 | 各步驟持久化欄位精確更新，刪除後 `findById` 回傳 `Optional.empty()` | 驗證 Entity 狀態生命週期（Managed / Detached / Removed） |
| **TC-LEDGER-IT-05** | 跨租戶橫向越權存取防禦 (IDOR) | 使用者 A 嘗試呼叫 `getRecord(recordB.getId(), userA.getId())` 或 `deleteRecord(...)` | 拋出 `ResourceNotFoundException` 或 `ForbiddenException`，且使用者 B 的記錄未被更動 | 驗證 SQL 層級之 `WHERE id = :id AND user_id = :userId` 租戶邊界 |
| **TC-LEDGER-IT-06** | 不存在分類建立記帳之邊界防禦 | 呼叫 `createRecord` 傳入隨機不存在的 `categoryId: 999999L` | 拋出 `ResourceNotFoundException`，資料庫無髒資料寫入 | 驗證關聯實體驗證與外鍵防禦 |

---

### 4.3 類別二：`CategoryServicePersistenceIT.java`（分類管理與關聯約束整合）

涵蓋分類唯一性、系統種子分類保護以及與流水帳之間的外鍵引用防禦：

| 案例編號 | 測試案例名稱 | 驗證情境與輸入數據 | 預期結果與核心斷言 | 防禦與技術價值 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-CAT-IT-01** | 系統種子分類唯讀保護 | 嘗試呼叫 `updateCategory` 或 `deleteCategory` 異動種子分類 (`isSystem = true`) | 拋出業務異常拒絕操作；重新查詢確認種子分類名稱與排序未變 | 防止惡意使用者或程式錯誤篡改全域基礎分類 |
| **TC-CAT-IT-02** | 多租戶自訂分類資料可見性隔離 | 使用者 A 新增自訂分類「攝影器材」；使用者 B 呼叫 `getAvailableCategories(userIdB)` | 使用者 B 僅可見系統種子分類與自身分類，絕對查不到「攝影器材」 | 驗證 JPQL `(c.isSystem = true OR c.userId = :userId)` 查詢精準度 |
| **TC-CAT-IT-03** | 分類關聯流水帳之刪除防禦 | 使用者建立自訂分類，並用其新增一筆流水帳；隨後嘗試刪除該自訂分類 | 系統檢測到 `countByCategoryId > 0`，拋出業務異常阻止刪除 | 避免資料庫孤兒資料 (Orphan Records) 與外鍵關聯異常 |
| **TC-CAT-IT-04** | 無關聯之自訂分類安全刪除 | 使用者建立自訂分類但未建立任何關聯記帳；呼叫 `deleteCategory` | 成功自資料庫刪除，再次查詢已不存在 | 驗證正常合法情境下自訂分類之完整生命週期 |
| **TC-CAT-IT-05** | 同一使用者同類型分類名稱重複防禦 | 使用者 A 嘗試建立兩次名稱同為「健身補劑」、類型同為 EXPENSE 的分類 | 第二次新增拋出 `ConflictException`，資料庫僅保留一筆 | 驗證 `existsByUserIdAndTypeAndName` 業務唯一性約束 |

---

### 4.4 類別三：`AuthServicePersistenceIT.java`（認證服務與使用者資料落盤整合）

涵蓋使用者註冊、密碼雜湊落盤、衝突防禦與交易完整性：

| 案例編號 | 測試案例名稱 | 驗證情境與輸入數據 | 預期結果與核心斷言 | 防禦與技術價值 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AUTH-IT-01** | 註冊成功與 BCrypt 雜湊落盤驗證 | 傳入合法註冊資料（帳號、密碼、姓名、Email） | 查詢資料庫實體：`passwordHash` 開頭為 `$2a$10$`，明文絕無落盤，ID 為正整數 | 驗證機密資料加密落地契約 |
| **TC-AUTH-IT-02** | 重複使用者帳號註冊衝突防禦 | 註冊使用者 `user_alpha` 成功後，再次以相同帳號註冊 | 拋出 `ConflictException`，第一次建立之使用者資料毫無損壞 | 驗證使用者名稱唯一鍵索引與服務衝突檢查 |
| **TC-AUTH-IT-03** | 重複 Email 註冊衝突防禦 | 以不同帳號但相同 Email 嘗試註冊 | 拋出 `ConflictException`，資料庫拒絕第二筆寫入 | 驗證電子郵件唯一性邊界防護 |

---

## 5. 雙資料庫環境切換與執行指令 (Execution Guide)

本套服務層持久化整合測試完美繼承專案的雙軌資料庫設計：

```bash
# 1. 預設極速模式 (使用 H2 In-Memory DB，符合 CI/CD 與日常開發快速驗收)
#    - 毫秒級執行完畢，無需本機安裝或啟動 MS SQL Server
.\mvnw.cmd verify -Dtest=*PersistenceIT

# 2. 指定單一測試類別執行
.\mvnw.cmd test -Dtest=LedgerServicePersistenceIT

# 3. 本機實體驗收模式 (連接至本機 MS SQL Server 2022 的 tibame_account_test 測試庫)
#    - 驗證 SQL Server 實體方言、IDENTITY 跳號自增特性與 Unicode 繁體中文存取
.\mvnw.cmd verify -Dtest=*PersistenceIT -Dspring.profiles.active=test-mssql
```

---

## 6. 後續推進計畫與 OpenSpec 變更提案指引 (Next Steps)

本探索報告已明確收斂服務層持久化整合測試的架構設計、抽象基底類別以及 14 個核心測試案例矩陣。

### 建議落地流程
1. **開立 OpenSpec 變更提案**：
   執行 `openspec new change "service-persistence-integration-tests"`。
2. **制定變更規範與任務清單**：
   - 建立 `specs/service-persistence-integration-testing/spec.md`。
   - 建立 `tasks.md` 包含：
     - Task 1: 建立 `ServiceIntegrationTestBase.java` 抽象基底類別。
     - Task 2: 實裝 `LedgerServicePersistenceIT.java`（6 大案例）。
     - Task 3: 實裝 `CategoryServicePersistenceIT.java`（5 大案例）。
     - Task 4: 實裝 `AuthServicePersistenceIT.java`（3 大案例）。
     - Task 5: 執行雙軌資料庫驗收（H2 + MS SQL Server）並確保全數綠燈通過。
3. **推進並歸檔**：
   依循 `/opsx-apply` 實裝完成後，更新正式規範文件並完成變更歸檔。
