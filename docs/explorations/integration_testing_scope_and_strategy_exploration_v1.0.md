# 每日記帳系統 (Daily Ledger System) - 整合測試範疇與實施策略探索報告 (v1.0 歷史存檔)

> **文件版本**：v1.0.0 (歷史存檔版本)  
> **建立日期**：2026-09-04  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / JUnit 5 / MockMvc / TestRestTemplate / DataJpaTest / H2 In-Memory DB  
> **最新版本指引**：請參閱最新的 [整合測試範疇與實施策略探索報告 (v2.0 最新版)](integration_testing_scope_and_strategy_exploration.md) ｜ [← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題意識 (Executive Summary & Background)

本專案「每日記帳系統（Daily Ledger System）」在工程品質上已落實嚴謹的雙軌驗證機制：
* **底層單元測試**（54 個案例）：完全基於 Mockito 隔絕外部環境，在 2~5 秒內極速反饋純 Java 運算邏輯。
* **頂層 E2E 測試**（10 個核心情境）：包含以 `TestRestTemplate` 執行的 API 整合測試與 Playwright 真機 UI 流程驗收。

然而，在單元測試與頂層 E2E 之間，存在一塊極為關鍵的**測試盲區**：
1. **單元測試「測不到」真實持久層行為**：單元測試中所有 JPA Repository 皆為 Mock，無法驗證真實 SQL 語法、H2 資料庫方言、`Specification` 動態條件組裝、JPQL 聚合計算（如 `COALESCE(SUM, 0)`）、資料庫唯一性約束（Unique Constraint）與外鍵引用的真實行為。
2. **單元測試「測不到」安全過濾鏈與交易邊界**：單元測試直接調用 Service 方法傳入 `userId`，無法驗證 `JwtAuthenticationFilter` 在遭遇無 Token、畸形 Token 或過期 Token 時是否能精確阻擋，亦無法檢驗 `@Transactional(rollbackFor = Exception.class)` 在業務拋出異常時是否真實回滾資料庫。
3. **E2E 測試「難以窮舉邊界且開銷過重」**：頂層真機測試啟動耗時長，若要用 E2E 覆蓋所有 DTO 欄位校驗、日期邊界與跨實體約束錯誤，將導致建置流水線過度緩慢。

因此，構建完善的**整合測試（Integration Testing）**體系，是鞏固系統穩定度、確保多租戶隔離防禦與資料一致性的必要環節。

---

## 2. 整合測試架構與檢驗分層

整合測試聚焦於驗證「組件之間的真實協同」與「跨層契約的正確性」：

```
+-----------------------------------------------------------------------------+
|                          整合測試檢驗邊界與組件流向                         |
+-----------------------------------------------------------------------------+
|                                                                             |
|   [ HTTP 測試端點 (MockMvc / TestRestTemplate) ]                            |
|        |                                                                    |
|        v                                                                    |
|   [ 1. 安全過濾鏈 ] --> JwtAuthenticationFilter (Token 解析 / 拒絕 / 畸形)   |
|        |                                                                    |
|        v                                                                    |
|   [ 2. 請求上下文 ] --> UserContext (ThreadLocal 注入 / 隔離 / 清理)         |
|        |                                                                    |
|        v                                                                    |
|   [ 3. 控制器層 ]   --> ApiController (@Valid 參數校驗 / DTO 綁定)           |
|        |                                                                    |
|        v                                                                    |
|   [ 4. 業務交易層 ] --> ServiceImpl (@Transactional 回滾 / 跨實體業務邏輯)   |
|        |                                                                    |
|        v                                                                    |
|   [ 5. 持久層/DB ]  --> JpaRepository (真實 SQL / Specification 動態條件 /   |
|                          外鍵約束 / 聚合 SUM 統計 / 唯一鍵衝突)             |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 3. 整合測試檢驗範疇矩陣 (6 大核心維度)

針對目前系統已實作之功能，整合測試應當重點覆蓋以下 6 大維度：

```
+-----------------------------------------------------------------------------+
|                         整合測試六大核心範疇全景                            |
+-----------------------------------------------------------------------------+
|                                                                             |
|   1. 身分認證與過濾器防線   4. 資料庫交易與回滾原子性                       |
|      (Filter/Token/401)        (@Transactional Rollback)                    |
|                                                                             |
|   2. 分類管理與關聯完整性   5. 跨租戶水平越權防護 (IDOR)                    |
|      (is_system/409/引用阻擋)  (findByIdAndUserId / 404/403)                |
|                                                                             |
|   3. 流水帳計算與動態查詢   6. 系統開機初始化相容性                         |
|      (Specification/JPQL/精確度)(DataInitializer 冪等性)                    |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### 3.1 身分認證與授權防護鏈 (Authentication & Security Integration)
* **待測組件**：`JwtAuthenticationFilter`、`TokenService`、`UserContext`、`AuthApiController`、`GlobalExceptionHandler`。
* **核心驗證場景**：
  1. **未授權攔截**：請求受保護端點時未帶 `Authorization` Header 或為空 ➔ 驗證回傳 `401 Unauthorized`，且**絕對不進入 Controller**。
  2. **畸形與無效 Token 阻擋**：缺少 `Bearer ` 前綴、非法字元、無效簽名或過期 Token ➔ 驗證回傳 401。
  3. **白名單放行**：`/api/v1/auth/login`、`/api/v1/auth/register`、靜態資源 `/lib/**` 無需 Token 即可順暢存取。
  4. **ThreadLocal 記憶體隔離與清理**：在 Filter 的 `finally` 區塊強制調用 `UserContext.clear()`，驗證在高併發或線程池重用情境下無身分洩漏。
  5. **DTO 參數驗證轉譯**：註冊密碼強度不足（缺大寫或特殊字元）、帳號為空等 ➔ 驗證 `@Valid` 攔截，由 `GlobalExceptionHandler` 封裝為標準 `ApiResponse(400, "密碼必須包含...")`。

### 3.2 分類管理與資料完整性約束 (Category & Integrity Integration)
* **待測組件**：`CategoryApiController`、`CategoryServiceImpl`、`CategoryRepository`。
* **核心驗證場景**：
  1. **混合分類查詢 (System + Custom)**：查詢 `GET /api/v1/categories?type=EXPENSE` 時，返回「系統預設分類 (`is_system = true`) + 該用戶自訂分類」，且完全過濾其他使用者的私有分類。
  2. **同用戶分類唯一性 (409 Conflict)**：同一使用者在同一收支類型下建立重複名稱分類 ➔ 驗證拋出 409；不同使用者建立同名分類 ➔ 驗證正常允許。
  3. **系統預設分類防竄改**：嘗試對 `is_system = true` 的分類調用 `PUT` 或 `DELETE` ➔ 驗證拋出 `403 Forbidden`。
  4. **跨實體引用刪除防護 (關鍵整合點)**：
     * 當分類已被 `AccountRecord` 引用時，嘗試刪除該分類 ➔ 驗證觸發 `accountRecordRepository.countByCategoryId(id) > 0`，拋出 409 衝突拒絕刪除。
     * 將關聯記帳刪除後 ➔ 再次刪除分類應成功執行並從資料庫抹除。

### 3.3 流水帳核心業務、金額精度與動態查詢 (Ledger Records & Dynamic Query)
* **待測組件**：`RecordApiController`、`LedgerServiceImpl`、`AccountRecordRepository`、`RegexSmartParserServiceImpl`。
* **核心驗證場景**：
  1. **收支類型與分類匹配校驗**：若記帳類型為 `INCOME` 但選用 `EXPENSE` 分類 ➔ 驗證拋出 400 Bad Request。
  2. **跨用戶分類引用防護**：用戶 A 記帳時若故意帶入用戶 B 的私有分類 ID ➔ 驗證 `findAvailableById` 判定無權存取，拒絕寫入。
  3. **金額運算精度 (BigDecimal)**：
     * 邊界值驗證：金額為 0 或負數被 `@DecimalMin("0.01")` 攔截。
     * 大金額（如 `99,999,999.99`）於 H2/MySQL 存取不產生溢位或捨入誤差。
  4. **JPA Specification 動態多條件組合（單元測試無法驗證）**：
     * 驗證 `startDate`、`endDate`、`recordType`、`categoryId`、`keyword` 模糊搜尋多條件動態拼接。
     * 驗證分頁 `Pageable` 倒序排序（`recordDate DESC, id DESC`）之真實 SQL 語法與分頁結果正確性。
  5. **月度統計聚合 (JPQL COALESCE & SUM)**：
     * 呼叫 `sumAmountByUserIdAndRecordTypeAndDateRange`：當月無紀錄時，驗證 `COALESCE(SUM, 0)` 正確回傳 0，不產生空指針。
     * 跨月邊界（如 1/31 與 2/1）資料篩選嚴密性與 `netBalance` 收支淨值計算。
  6. **自然語言智慧記帳整合**：呼叫 `/api/v1/records/quick` 傳入「午餐 120」➔ 驗證解析、推導分類、最終落庫並可被查詢。

### 3.4 資料庫交易與回滾原子性 (Transaction & Atomicity Integration)
* **待測組件**：服務層標註 `@Transactional(rollbackFor = Exception.class)` 之方法。
* **核心驗證場景**：
  * 模擬在業務流程中途拋出 `ApiException` 或 `RuntimeException` 時，驗證前面已執行的 JPA 異動是否 100% 回滾，資料庫絕不留下半套殘留資料。

### 3.5 跨租戶橫向越權攻擊穿透防護 (Tenant Isolation / IDOR Security)
* **待測組件**：全 API 端點與資料存取層。
* **核心驗證場景**：
  * 用戶 B 嘗試讀取、修改或刪除用戶 A 的分類與流水帳紀錄 ➔ 驗證底層查詢強制綁定 `findByIdAndUserId(id, userId)`，一律回傳 404 Not Found 或 403 Forbidden，杜絕物件引用越權（IDOR）。

### 3.6 系統啟動預設資料與相容性 (Bootstrap & Schema Integration)
* **待測組件**：`DataInitializer`、`AppConfig`。
* **核心驗證場景**：
  * **冪等性 (Idempotency)**：全新資料庫開機自動植入 11 筆系統預設分類；二次開機或重複執行時略過，確保不引發主鍵重複或資料重複膨脹。

---

## 4. 實施途徑深度評估 (Approach A vs Approach B)

在落實整合測試時，主流有兩種技術架構策略：

```
+-----------------------------------------------------------------------------------+
|               途徑 A：切片整合測試 vs 途徑 B：全上下文整合測試                    |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   途徑 A (Slice Testing)                途徑 B (Full Context Testing)             |
|   - @WebMvcTest (Mock Service)          - @SpringBootTest (真實組裝)              |
|   - @DataJpaTest (Mock DB/僅測 SQL)     - MockMvc / TestRestTemplate              |
|                                                                                   |
|   [優點]                                [優點]                                    |
|   + 速度極快 (單切片 1~2 秒)            + 真實度最高 (High Fidelity)              |
|   + 邊界/異常輸入測試成本低             + 零 Mock 成本，無契約脫節風險            |
|   + 精準定位失敗層級                    + 原生驗證 Filter 鏈與 UserContext        |
|                                         + 完整驗證 Transaction 回滾與跨實體約束   |
|                                                                                   |
|   [缺點]                                [缺點]                                    |
|   - Mock 負擔大 (Mock Fatigue)          - 啟動耗時較長 (約 4~8 秒)                |
|   - 契約脫節引發「虛假綠燈」            - 需管理 H2 資料庫狀態與清理機制          |
|   - 無法驗證 Filter 與交易回滾          - 故障排查需順藤摸瓜                      |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 綜合維度評估對照表

| 評估維度 | 途徑 A：切片整合測試 (`@WebMvcTest` + `@DataJpaTest`) | 途徑 B：全上下文整合測試 (`@SpringBootTest` + `MockMvc`) |
| :--- | :--- | :--- |
| **執行反饋速度** | **極快**（單切片通常 1~2 秒完成，只載入局部 Bean） | **中等**（完整啟動 Context 約 4~8 秒，若重用 Context 則後續較快） |
| **Mock 負擔** | **高**（`@WebMvcTest` 需逐一 `@MockBean` Service；`@DataJpaTest` 無法測 Service 邏輯） | **零或極低**（各層直接真實注入，無需撰寫冗長的 Mock 打樁程式碼） |
| **全鏈路真實度** | **中等**（各層分別隔離，難以驗證跨層協同契約變更） | **極高**（真實貫穿 Filter ➔ Controller ➔ Service ➔ Repo ➔ H2） |
| **安全過濾鏈檢驗** | **較困難**（需手動將 `JwtAuthenticationFilter` 與 `TokenService` 納入 Web 切片） | **極佳**（原生支援完整 Filter 鏈與 `UserContext` 生命週期） |
| **業務交易 (`@Transactional`)** | **無法驗證**（Service 被 Mock，無法測試拋錯時的真實 Rollback 行為） | **完整支援**（真實檢驗 RuntimeException 觸發的交易回滾與原子性） |
| **跨實體約束檢驗** | **僅能單點驗證**（無法測「刪除分類時檢查流水帳引用」之真實連動） | **完整支援**（先建帳再刪分類，能真實驗證 `countByCategoryId` 攔截） |
| **SQL / Specification 支援** | **完美支援**（`@DataJpaTest` 專注於 Criteria 組合查詢與語法） | **完整支援**（直接透過 H2 執行完整 SQL 與聚合查詢） |
| **故障定位難易度** | **容易精準定位**（Web 切片報錯必屬 HTTP/校驗；JPA 切片報錯必屬 SQL） | **需順藤摸瓜**（失敗時可能需排查 Filter、Service 業務條件或 DB 約束） |

---

## 5. 本專案最佳實踐策略 (Hybrid 混合架構)

綜合評估本專案「後端四層嚴格解耦」、「自訂安全過濾鏈」與「多租戶嚴格隔離」之架構特徵，建議採取 **Hybrid（混合）測試策略**：

```
+-----------------------------------------------------------------------------------+
|                         專案推薦之測試策略分工 (Hybrid Strategy)                  |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. [ 主力防線 ] @SpringBootTest + MockMvc                                        |
|     - 用途：全鏈路 API 整合測試 (覆蓋認證過濾、Controller、Service、交易回滾、H2)  |
|     - 特點：以記憶體 DispatcherServlet 調用，不佔用真實埠，速度比 E2E 快數倍      |
|     - 範例：AuthIntegrationIT、CategoryIntegrationIT、LedgerIntegrationIT        |
|                                                                                   |
|  2. [ 專門武器 ] @DataJpaTest (輕量持久層切片)                                    |
|     - 用途：專案最複雜的 AccountRecordRepository Specification 動態條件查詢        |
|     - 特點：利用 H2 記憶體 DB 極速驗證 Criteria 多條件排列組合與 JPQL COALESCE 聚合 |
|     - 範例：AccountRecordRepositoryIT                                             |
|                                                                                   |
|  3. [ 既有防線維持 ]                                                              |
|     - 純單元測試 (Surefire 54 案例)：毫秒級極速反饋業務公式與加密演算法           |
|     - 真機 E2E 測試 (Playwright 5 類別)：交付前全站 UI 渲染與使用者旅程驗收        |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 6. 後續推進與工作流建議 (Next Steps & Roadmap)

1. **命名與生命週期分流**：
   * 整合測試類別遵循 Maven 標準命名以 `*IT.java` 結尾（如 `CategoryIntegrationIT.java`）。
   * 納入 Maven Failsafe 生命週期，執行 `mvn verify` 或 `mvn test-compile failsafe:integration-test` 統一調度。
2. **規格收斂至 OpenSpec**：
   * 若團隊決定正式編寫整合測試代碼，可透過 `/opsx-propose` 發起 `add-backend-integration-tests` 變更提案，明確定義任務清單與交付產物。
