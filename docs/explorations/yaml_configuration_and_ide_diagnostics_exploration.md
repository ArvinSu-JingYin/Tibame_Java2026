# 每日記帳系統 (Daily Ledger System) - YAML 設定檔規範、IDE 診斷排除與 JWT 強型別配置探索報告

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Spring Boot 3.3.13 / Spring Tools 4 / YAML Relaxed Binding / JJWT 0.12.6  
> **目標範疇**：YAML 映射鍵轉義、Configuration Metadata 機制、`@ConfigurationProperties` 強型別重構  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題現象 (Background & Diagnostics Summary)

在開發與維護專案的整合測試設定檔 [`application-test.yml`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/test/resources/application-test.yml) 時，VS Code 擴充套件 **Spring Boot Tools (`vscode-spring-boot` / Spring Tools 4)** 在 Problems 面板中回報了 5 筆提示與診斷警告：

```
+---------------------------------------------------------------------------------------------------------------+
|                                      IDE 診斷訊息與警告盤點矩陣                                                |
+------+--------------------------+-----------------------+----------+------------------------------------------+
| 行號 | 欄位/鍵名 (Key)          | 警告代碼              | 嚴重度   | IDE 提示訊息摘要                         |
+------+--------------------------+-----------------------+----------+------------------------------------------+
| L15  | format_sql               | YAML_SHOULD_ESCAPE    | Hint (4) | This key is used in a map and contains   |
|      |                          |                       |          | special characters. Escape with '[]'.    |
| L34  | jwt                      | YAML_UNKNOWN_PROPERTY | Hint (4) | Unknown property 'jwt'.                  |
| L40  | com.tibame               | YAML_SHOULD_ESCAPE    | Hint (4) | This key is used in a map and contains   |
|      |                          |                       |          | special characters. Escape with '[]'.    |
| L41  | org.springframework.web  | YAML_SHOULD_ESCAPE    | Hint (4) | This key is used in a map and contains   |
|      |                          |                       |          | special characters. Escape with '[]'.    |
| L42  | org.hibernate.SQL        | YAML_SHOULD_ESCAPE    | Hint (4) | This key is used in a map and contains   |
|      |                          |                       |          | special characters. Escape with '[]'.    |
+------+--------------------------+-----------------------+----------+------------------------------------------+
```

### 影響評估
- **執行期正常**：Spring Boot 的底層 Relaxed Binding 機制與 `@Value("${jwt.secret}")` 在執行期仍可正常運作，單元測試與 E2E 測試皆可通過。
- **維護性與體驗受損**：
  1. 編輯器出現黃色波浪底線與檢查提示，增加開發者的視覺認知負載。
  2. YAML 中缺乏元資料（Metadata）感知，失去自動補全、型別校驗與快速跳轉功能。
  3. 未轉義的 Map 鍵值在特定 Spring Boot 版本或環境變數覆寫時，存在潛在的解析歧義風險。

---

## 2. 根本成因深度剖析 (Root Cause Analysis)

```
+-----------------------------------------------------------------------------------------+
|                                    Spring Tools 4 診斷機制                              |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
| 1. YAML_SHOULD_ESCAPE (轉義警告)                                                        |
|    Map 鍵名含有 "." (點號) 或 "_" (底線)                                                 |
|    --> 容易在 YAML 階層解析與 Relaxed Binding 時與「巢狀屬性」或「環境變數轉換」混淆      |
|    --> Spring Boot 官方規範要求使用括號語法包裹："[key]"                                |
|                                                                                         |
| 2. YAML_UNKNOWN_PROPERTY (未知屬性)                                                     |
|    IDE 透過 META-INF/spring-configuration-metadata.json 識別配置項                      |
|    --> 目前專案在 JwtTokenServiceImpl 採用 @Value 散裝注入                               |
|    --> 缺少 @ConfigurationProperties 與 spring-boot-configuration-processor              |
|    --> IDE 判定為未宣告的前綴屬性                                                      |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 2.1 `YAML_SHOULD_ESCAPE` 的產生機制
在 Spring Boot 的配置體系中：
- `spring.jpa.properties` 綁定型別為 `Map<String, Object>`。
- `logging.level` 綁定型別為 `Map<String, LogLevel>`。

當 YAML 中的映射鍵名包含特殊字元時：
1. **點號 `.`**（如 `com.tibame`、`org.springframework.web`）：YAML 解析器或 Spring 寬鬆綁定容易誤將其視為多層巢狀結構（Object Property Traversal）。
2. **底線 `_`**（如 `format_sql`）：在寬鬆綁定中，底線常被映射為系統環境變數的大寫轉小寫分隔符號。

**官方規範解答**：Spring Boot 官方文件明確建議，當 Map 的 Key 包含點號、底線或非標準字元時，應使用中括號包裹鍵名 `"[...]"`（Bracket Notation），確保 YAML 解析器明確將其視為不可分割的單一字串鍵值。

### 2.2 `YAML_UNKNOWN_PROPERTY` 的產生機制
VS Code Spring Boot Tools 是透過載入 Spring Boot 及其相依套件編譯時產生的中繼資料檔案（`META-INF/spring-configuration-metadata.json`）來對 YAML/Properties 進行語法校驗與補全。

目前專案中，JWT 相關配置是以 `@Value` 方式零散注入於 [`JwtTokenServiceImpl.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/common/crypto/token/impl/JwtTokenServiceImpl.java)：
```java
public JwtTokenServiceImpl(
    @Value("${jwt.secret:...}") String secret,
    @Value("${jwt.expiration-ms:86400000}") long expirationMs)
```
因為沒有建立任何對應前綴 `jwt` 的 `@ConfigurationProperties` 類別，也沒有引入 `spring-boot-configuration-processor`，因此編譯期完全未生成 `jwt` 相關的 Metadata，IDE 即認定 `jwt` 為未定義的無效屬性。

---

## 3. 方案評估與決策對照 (Options Comparison & Decision)

```
+-----------------------------------------------------------------------------------------------+
|                                      修復方案對比矩陣                                          |
+----------------------+------------------------------------+-----------------------------------+
| 評估維度             | 方案 A：完整規範化修復 (推薦)      | 方案 B：輕量級快速修復            |
+----------------------+------------------------------------+-----------------------------------+
| 核心做法             | 1. YAML Map 鍵轉義                 | 1. 僅修改 YAML Map 鍵轉義         |
|                      | 2. 引入配置處理器與 JwtProperties  | 2. IDE 端忽略未知屬性警告         |
| 架構潔淨度           | 高（符合 Spring Boot 企業最佳實踐）| 中（維持散裝 @Value 注入）        |
| 型別安全與驗證       | 強（編譯期型別檢查、預設值集中化）  | 弱（字串解析、分散各處）          |
| IDE 智慧補全         | 完整支援（提示說明、預設值、補全） | 無支援（依舊無感知）              |
| 影響範圍             | pom.xml、JwtProperties、Service    | 僅 application-*.yml              |
| 解決完整度           | 100% 消除全部 5 筆診斷提示         | 消除 4 筆，1 筆需透過 IDE 配置隱藏|
+----------------------+------------------------------------+-----------------------------------+
```

### 決策：採納「方案 A（完整規範化修復）」
方案 A 不但徹底消滅所有診斷警告，更是現代 Spring Boot 3.x 推薦的配置管理模式：將散落的 `@Value` 聚合為結構化、可重複驗證的 `@ConfigurationProperties` 類別。

---

## 4. 方案 A 實施規格與詳細代碼對照 (Implementation Specifications)

### 步驟 1：在 `pom.xml` 引入 Configuration Processor
在 [`pom.xml`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/pom.xml) 中新增 `spring-boot-configuration-processor`，標註為 `<optional>true</optional>`，確保不隨應用打包至生產運行包：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

---

### 步驟 2：建立強型別配置類別 `JwtProperties`
於 `com.tibame.config` 套件下建立強型別配置類：

```java
package com.tibame.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 權杖簽署與驗證配置屬性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC-SHA 簽署密鑰 (至少需符合 SHA-256 密鑰長度要求)
     */
    private String secret = "SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026";

    /**
     * 權杖有效存活時間 (毫秒)
     */
    private long expirationMs = 86400000L;
}
```

---

### 步驟 3：重構 `JwtTokenServiceImpl` 注入方式
在 [`JwtTokenServiceImpl.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/common/crypto/token/impl/JwtTokenServiceImpl.java) 中注入 `JwtProperties`，並保留便利的重載建構子，確保現有單元測試（如 [`TokenServiceTest.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/test/java/com/tibame/TokenServiceTest.java)）完全不需要重寫：

```java
@Slf4j
@Service
public class JwtTokenServiceImpl implements TokenService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Spring 容器注入專用建構子
     */
    @Autowired
    public JwtTokenServiceImpl(JwtProperties jwtProperties) {
        this(jwtProperties.getSecret(), jwtProperties.getExpirationMs());
    }

    /**
     * 單元測試或自訂參數專用建構子
     */
    public JwtTokenServiceImpl(String secret, long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        log.info("初始化 JWT 權杖簽署服務 (HMAC-SHA, Expiration: {} ms)", expirationMs);
    }

    // ... 業務邏輯保持不變 ...
}
```

---

### 步驟 4：規範化 YAML 設定檔 (Escape Bracket Notation)

#### 4.1 測試設定檔 [`application-test.yml`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/test/resources/application-test.yml)
```diff
   jpa:
     hibernate:
       ddl-auto: create-drop
     show-sql: false
     open-in-view: false
     properties:
       hibernate:
-        format_sql: true
+        "[format_sql]": true

   # ... jwt 維持不變，已有 Metadata 感知 ...
   jwt:
     secret: E2ETestSecretKeyForDailyLedgerSwissArchitecture2026!#TestingOnlySuperSecretKey
     expiration-ms: 3600000 # 1 hour for test suite

   logging:
     level:
-      com.tibame: INFO
-      org.springframework.web: WARN
-      org.hibernate.SQL: WARN
+      "[com.tibame]": INFO
+      "[org.springframework.web]": WARN
+      "[org.hibernate.SQL]": WARN
```

#### 4.2 主設定檔 [`application.yml`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/application.yml)
同步修正主設定檔以維持全域規範一致：
```diff
   jpa:
     hibernate:
       ddl-auto: update
     show-sql: false
     open-in-view: false
     properties:
       hibernate:
-        format_sql: true
+        "[format_sql]": true

   logging:
     level:
-      com.tibame: DEBUG
-      org.springframework.web: INFO
-      org.hibernate.SQL: INFO
+      "[com.tibame]": DEBUG
+      "[org.springframework.web]": INFO
+      "[org.hibernate.SQL]": INFO
```

---

## 5. 驗證檢核與 DoD 檢驗項目 (Verification & DoD)

實施完成後應進行以下檢驗，確保完全符合瑞士風格極簡與零警告要求：

1. **IDE Problems 面板檢核**：
   - 打開 `application-test.yml` 與 `application.yml`。
   - 確認 `YAML_SHOULD_ESCAPE` 與 `YAML_UNKNOWN_PROPERTY` 均徹底消失，Problems 面板達到零警告（Zero Warnings）。
2. **IDE 智慧補全測試**：
   - 在 YAML 檔案中輸入 `jwt.`，確認 IDE 能自動帶出 `jwt.secret` 與 `jwt.expiration-ms`，並顯示 JavaDoc 說明與預設值。
3. **單元測試與 E2E 測試驗證**：
   - 執行 `mvn test` 或透過 VS Code 執行：
     - `TokenServiceTest`：驗證多載建構子運作無誤。
     - `AuthApiE2ETest`：驗證 E2E 整合測試讀取 `application-test.yml` 正確注入 1 小時過期時間與專屬測試密鑰。

---

## 6. 後續銜接與 OpenSpec 流程 (Next Steps)

本探索報告已正式收錄於專案探索文件軌。後續欲將此方案實作落地時，可執行：
1. 透過 `/opsx-propose` 或建立 OpenSpec 變更提案（例：`standardize-yaml-and-jwt-properties`）。
2. 依序執行任務：引入 Processor、建立 `JwtProperties`、重構 Service、更新 YAML、驗證測試。
