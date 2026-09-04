# 本機 MS SQL 測試資料庫 (tibame_account_test) 介接與程式碼影響性探索報告 (MSSQL Test Database Integration and Codebase Impact Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Spring Boot 3.3.13 / Spring Data JPA / Hibernate 6.5 / MS SQL Server 2022 / Maven Failsafe / JUnit 5  
> **目標範疇**：本機已新增 `tibame_account_test` 資料庫之程式碼庫影響分析、設定檔規格規劃、測試隔離防禦架構  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **關聯文件**：[整合測試範疇、安全鏈路校準、單一Context與SQL Server介接策略報告 (integration_testing_scope_and_strategy_exploration.md)](integration_testing_scope_and_strategy_exploration.md) / [本機資料庫密碼零改動注入與 IDE 隔離機制探索報告 (local_database_credentials_and_ide_injection_exploration.md)](local_database_credentials_and_ide_injection_exploration.md)

---

## 1. 探索背景與核心結論 (Background & Core Conclusion)

在日常流水帳系統開發中，工程師已於本機 Microsoft SQL Server 實例上成功手動建立了測試專用資料庫 `tibame_account_test`。本探索報告旨在深入評估：**「此新資料庫的加入，對專案既有程式碼庫（業務代碼、測試代碼、配置檔、建置腳本）的具體影響範圍是什麼？需要修改哪些部分？」**

### 1.1 核心結論宣告 (Executive Conclusion)

> [!IMPORTANT]
> **業務核心程式碼（`src/main/java`）完全「100% 零修改」！**
> 
> 專案採用 Spring Data JPA 與 Hibernate 作為持久層抽象化標準，所有 Entity、Repository、Service、Controller、DTO 與 Security 邏輯皆與實體資料庫名稱解耦。日常開發庫 `tibame_account` 與測試專用庫 `tibame_account_test` 僅在底層 JDBC DataSource 連線字串上有所區分，在 Java 程式碼層次完全透明。

### 1.2 異動範疇界定 (Scope Definition)

所有具體需要**新增**或**調整**的內容，嚴格收斂在以下四個維度：
1. **測試組態層**：新增專屬 Profile 設定檔 `application-test-mssql.yml`。
2. **測試基底層**：建立具備交易自動回滾（`@Transactional`）的測試基底類別 `IntegrationTestBase.java`。
3. **測試案例寫法**：消除 SQL Server `IDENTITY` 跳號盲區，嚴格採用動態 ID 斷言。
4. **開發與除錯輔助**：更新 VS Code `launch.json.example` 與 Maven 隨選執行指令。

---

## 2. 雙環境與雙資料庫架構藍圖 (Dual Database & Dual Profile Architecture)

為兼顧「極速開發反饋」與「真實 SQL Server 相容性深度除錯」，系統採行嚴格的環境與資料庫隔離分流：

```
+--------------------------------------------------------------------------------------------------+
|                                    系統資料庫與設定檔對照藍圖                                    |
+--------------------------------------------------------------------------------------------------+
|                                                                                                  |
|   [ 業務開發 / 運行環境 (Runtime) ]                  [ 測試與自動化驗證環境 (Testing) ]          |
|                                                                                                  |
|   +-------------------------------+                 +--------------------------------+           |
|   |  Profile: mssql (本機實體)    |                 |  Profile: test (預設 CI / PR)  |           |
|   |  application-mssql.yml        |                 |  application-test.yml          |           |
|   +---------------+---------------+                 +---------------+----------------+           |
|                   |                                                 |                            |
|                   v (直連)                                          v (記憶體模式)               |
|      [ MS SQL Server 實體庫 ]                         [ 內嵌 H2 記憶體虛擬庫 ]                   |
|      庫名: tibame_account                              庫名: mem:tibame_account_test              |
|      (保留日常開發與手動測試資料)                       (單元測試、PR Gate ~5 秒極速反饋)         |
|                                                                                                  |
|                                                     +--------------------------------+           |
|                                                     |  Profile: test-mssql (隨選真機)|           |
|                                                     |  application-test-mssql.yml    | <== 新增  |
|                                                     +---------------+----------------+           |
|                                                                     |                            |
|                                                                     v (直連)                     |
|                                                       [ MS SQL Server 實體測試庫 ]               |
|                                                       庫名: tibame_account_test       <== 已建庫 |
|                                                       (自動建表、交易回滾、排查語法)             |
|                                                                                                  |
+--------------------------------------------------------------------------------------------------+
```

### 2.1 嚴格隔離的必要性 (Why Strict Isolation Matters)

* **防範資料滅頂災難**：自動化測試通常具有資料清理、重置或大量的測試垃圾資料。若測試直接對接日常開發庫 `tibame_account`，將抹除開發者精心維護的手動測試資料與使用者帳號。
* **無副作用隨選排查**：測試庫 `tibame_account_test` 專門搭配 `@Transactional` 回滾機制，提供一個專屬的「沙盒資料庫」，既能檢驗真實 SQL Server 方言、約束與觸發行為，又保證開機即乾淨。

---

## 3. 程式碼庫異動全景盤點矩陣 (Codebase Mutation Matrix)

| 系統層次 | 目標路徑 | 異動狀態 | 具體影響與職責說明 |
| :--- | :--- | :---: | :--- |
| **業務代碼層** | `src/main/java/**` | **零更動** | Controller、Service、Repository、Entity 等無須任何調整。 |
| **正式配置層** | `src/main/resources/application-mssql.yml` | **維持不變** | 依然維持指向本機開發庫 `databaseName=tibame_account`。 |
| **DDL/種子腳本** | `src/main/resources/schema.sql`<br>`src/main/resources/data.sql` | **維持不變** | 腳本內建 `IF NOT EXISTS` 冪等防護，可直接被測試庫安全調用。 |
| **測試配置層** | `src/test/resources/application-test-mssql.yml` | **【全新建立】** | 配置對接 `tibame_account_test`，啟用開機自動執行 DDL 與種子資料。 |
| **測試基底層** | `src/test/java/com/tibame/integration/base/IntegrationTestBase.java` | **【建議新增】** | 定義整合測試共同 Context，配置 `@Transactional` 確保測試後自動回滾。 |
| **測試案例層** | `src/test/java/**/integration/*IT.java` | **【規範遵循】** | 斷言嚴禁硬編碼 ID（如 `1L`），改採動態 ID 斷言相容 SQL Server `IDENTITY`。 |
| **IDE 輔助層** | `.vscode/launch.json.example` | **【更新範本】** | 新增以 `test-mssql` Profile 啟動或除錯的範本配置。 |
| **維運腳本層** | `start.ps1` | **【選配擴充】** | 可選擴充 `-Profile mssql-test` 參數，供手動將整個 Web 服務連至測試庫。 |

---

## 4. 關鍵配置與實作規格深入剖析 (Technical Specifications)

### 4.1 新增專屬測試設定檔 (`src/test/resources/application-test-mssql.yml`)

當執行包含 `-Dspring.profiles.active=test-mssql` 的指令時，Spring Boot 會自動載入此檔案：

```yaml
spring:
  datasource:
    # 核心：指向本機手動新建的 tibame_account_test 資料庫
    url: ${DB_TEST_URL:jdbc:sqlserver://localhost:1433;databaseName=tibame_account_test;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true;}
    username: ${DB_TEST_USERNAME:${DB_USERNAME:sa}}
    password: ${DB_TEST_PASSWORD:${DB_PASSWORD:1111}}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver

  jpa:
    hibernate:
      ddl-auto: update # 與 schema.sql 雙重保障表結構正確映射
    show-sql: true
    properties:
      hibernate:
        "[format_sql]": true
        dialect: org.hibernate.dialect.SQLServerDialect

  sql:
    init:
      mode: always # 核心關鍵：新庫若是空的，啟動時自動執行 schema.sql 與 data.sql
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
      continue-on-error: true # 遇已存在物件安全跳過，保證冪等

jwt:
  secret: SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026
  expiration-ms: 3600000 # 測試期 Token 有效期 1 小時

logging:
  level:
    "[com.tibame]": DEBUG
    "[org.hibernate.SQL]": DEBUG
    "[org.hibernate.orm.jdbc.bind]": TRACE # 可選：印出 SQL 綁定參數以利疑難排查
```

#### 設計精華解析
1. **零手動 DDL 負擔（Zero DDL Manual Work）**：
   使用者在 SQL Server 建立 `tibame_account_test` 後不需要手動執行任何建表語法。透過 `spring.sql.init.mode: always`，Spring Boot 首次連線該庫時，會自動執行專案現有的 [`schema.sql`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/schema.sql) 與 [`data.sql`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/data.sql)，自動生成 `sys_user`、`sys_category`、`account_record` 以及 11 筆系統預設分類。
2. **彈性機密繼承**：
   支援 `DB_TEST_PASSWORD` 覆寫，若未指定則無縫回退至 `DB_PASSWORD` 或本機預設 `1111`，相容個人化安全設定。

---

### 4.2 整合測試基底設計 (`IntegrationTestBase.java`)

```java
package com.tibame.integration.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 整合測試共用基底類別
 * 預設載入 test profile (H2 極速模式)
 * 透過命令列傳入 -Dspring.profiles.active=test-mssql 可平滑切換至本機 SQL Server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional // 核心關鍵：測試方法完成後自動 ROLLBACK TRANSACTION，保持 tibame_account_test 永遠潔淨
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    // 可定義通用之輔助方法，如 obtainAccessToken(...) 或 mockUserLogin(...)
}
```

---

## 5. SQL Server 真機整合測試防禦最佳實踐 (Defensive Testing Practices)

當測試對象由 H2 轉向真實微軟 SQL Server 時，必須恪守以下四大防禦準則：

```
+-----------------------------------------------------------------------------------+
|                        SQL Server 整合測試防禦最佳實踐                            |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. [ 資料庫嚴格隔離 ] ───> 專用 tibame_account_test 庫，絕不可動用開發或正式庫   |
|                                                                                   |
|  2. [ 主鍵絕不硬編碼 ] ───> SQL Server IDENTITY 跳號特性，Assert 僅檢驗 isNotNull |
|                                                                                   |
|  3. [ 交易自動回滾 ]   ───> 測試方法標註 @Transactional，測完資料庫自動 Rollback  |
|                                                                                   |
|  4. [ 開機 DDL 冪等性 ] ───> schema.sql 使用 IF NOT EXISTS，支援重用與平滑升級     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 5.1 痛點示警：`IDENTITY(1,1)` 跳號機制
* **現象**：在 SQL Server 中，若某一筆 `INSERT` 在 `@Transactional` 回滾後，其被佔用的 `IDENTITY` 值**不會退回**。下一次新增紀錄時，主鍵 ID 將直接跳號（例如從 `1` 跳到 `2`）。
* **防禦對策**：
  * **危險寫法（必爆錯）**：
    ```java
    // 錯誤：假設第一筆資料 ID 一定為 1L
    assertEquals(1L, savedUser.getId());
    ```
  * **標準寫法（安全）**：
    ```java
    // 正確：僅驗證 ID 由資料庫成功指派，並以動態 ID 作為後續查詢依據
    assertThat(savedUser.getId()).isNotNull().isPositive();
    Long dynamicId = savedUser.getId();
    ```

---

## 6. 調度指令與 IDE 實踐手冊 (Execution & IDE Cheatsheet)

### 6.1 命令列調度速查

```powershell
# 1. 日常快速驗證 (秒級反饋，執行單元測試，無須 SQL Server)
./mvnw test

# 2. 隨選整合測試 - 預設 H2 記憶體模式 (~5 秒)
./mvnw test-compile failsafe:integration-test -Dit.test="*IT"

# 3. 隨選整合測試 - 直連本機 SQL Server (tibame_account_test) (~8 秒)
./mvnw test-compile failsafe:integration-test -Dspring.profiles.active=test-mssql -Dit.test="*IT"

# 4. 全量驗收 (Surefire 單元測試 + Failsafe 真機/端到端測試)
./mvnw verify
```

### 6.2 VS Code 測試與偵錯配置範本 (`.vscode/launch.json.example`)

可於 `.vscode/launch.json` 擴充以下偵錯項：

```json
{
  "type": "java",
  "name": "Debug: 整合測試 (連線本機 tibame_account_test)",
  "request": "launch",
  "mainClass": "com.tibame.integration.AuthIntegrationIT",
  "projectName": "daily-ledger-system",
  "vmArgs": "-Dspring.profiles.active=test-mssql"
}
```

---

## 7. 總結與後續推進 (Summary & Next Steps)

本機新增 `tibame_account_test` 資料庫是專案邁向**高可靠度真實環境測試架構**的重要里程碑。透過外部化設定檔分流，專案達成了：
1. **零程式碼侵入**：業務代碼無痛支援多庫切換。
2. **零環境污染**：開發庫與測試庫各司其職，交易自動回滾不殘留垃圾數據。
3. **開箱即用**：初次開機自動完成 DDL 表格建立與系統種子資料注入。

當您準備好將上述規格正式落地至程式碼庫時，建議退出探索模式，並透過 `/opsx-propose` 發起例如 `configure-mssql-test-database` 變更提案進行自動化落地與驗收！
