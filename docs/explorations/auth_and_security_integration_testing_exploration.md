# 認證、授權與安全性邊界整合測試探索報告 (Auth & Security Integration Testing Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Java 21 / Spring Boot 3.3.13 / MockMvc / JUnit 5 / JJWT 0.12.6 / H2 In-Memory DB / MS SQL Server 2022  
> **目標範疇**：整合測試首發模組（Suite 1）、測試基底 Fixture 擴充、401/400 安全攔截與密碼政策校驗  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md) ｜ [母報告：整合測試範疇與實施策略總綱 (v3.0)](integration_testing_scope_and_strategy_exploration_v3.0.md) ｜ [單元測試手冊](../specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md)  
> **後續落地**：`openspec/changes/add-auth-security-integration-tests`  

---

## 1. 探索背景與核心使命 (Executive Summary & Mission)

在「每日記帳系統（Daily Ledger System）」的測試體系中，底層單元測試已完成 66 個案例的覆蓋（含 BCrypt 演算法、密碼政策校驗邏輯與 JWT 工具方法），但在中層整合測試（Integration Testing）維度，仍缺乏對 **真實 Spring 容器環境下 HTTP 請求處理流向** 的完整檢驗。

本探索報告聚焦於「4 大整合測試套件模組劃分」中的**首發先導模組（Suite 1）**，肩負雙重核心使命：
1. **基礎設施升級（Level 2）**：擴充共用測試抽象基底 `IntegrationTestBase.java`，實裝高頻測試用戶工廠（`createTestUser`）、JWT 快速簽署與 JSON 序列化工具，將單個案例身分準備耗時壓縮至 **1 毫秒內**。
2. **安全領域實裝（Level 3）**：建立 `AuthIntegrationIT.java`，直接針對 `/api/v1/auth/*` 端點進行灰箱檢驗，全面補齊單元測試無法覆蓋的 **Controller `@Valid` 參數校驗（盲區 3）** 與 **安全防護邊界攔截（盲區 4）**。

```
+-----------------------------------------------------------------------------------+
|                        專案整合測試推進進度與 Suite 1 落點                        |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ Level 1：環境與連線基礎設施 ] ───> 【現況：已完成 [x]】                        |
|  - application-test-mssql.yml (tibame_account_test 專屬庫連線)                    |
|  - IntegrationTestBase.java (@SpringBootTest + @AutoConfigureMockMvc)             |
|  - DatabaseIntegrationIT.java (驗證種子資料載入與 IDENTITY 動態主鍵)              |
|                                                                                   |
|  [ Level 2：共用測試輔助工具 (Fixtures & Helpers) ] ───> 【本模組重點升級】       |
|  - 擴充 IntegrationTestBase：預算 BCrypt 雜湊、1ms Token 簽署、JSON 工具           |
|                                                                                   |
|  [ Level 3：Suite 1 安全邊界實裝 ] ───> 【本模組核心交付】                         |
|  - 實作 AuthIntegrationIT.java (5 大案例：401 攔截、偽造 Token、密碼政策校驗)      |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. 安全防護鏈真實流向與例外轉譯機制 (Security Architecture Reality)

在設計整合測試斷言時，必須精確反映本系統的真實安全過濾架構，而非套用傳統 Spring Security 預設認知：

```
+----------------------------------------------------------------------------------------------------+
|                               安全防護鏈與例外轉譯真實鏈路全景                                     |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ HTTP 請求 (MockMvc) ]                                                                           |
|       |                                                                                            |
|       v                                                                                            |
|  [ 1. JwtAuthenticationFilter ] (OncePerRequestFilter)                                             |
|       | - 解析 Header: Authorization: Bearer <token>                                               |
|       | - 若合法: 解析 userId/username 寫入 UserContext (ThreadLocal)                               |
|       | - 若無 Token 或 Token 畸形/過期: 不中斷請求，直接放行 (filterChain.doFilter)               |
|       v                                                                                            |
|  [ 2. 進入 ApiController 端點 ]                                                                    |
|       | - 先觸發 Spring MVC @Valid 參數校驗 (若不符規則直接拋出 MethodArgumentNotValidException)   |
|       | - 業務開頭顯式呼叫: Long userId = UserContext.requireUserId();                             |
|       | - 若 UserContext 為 null ➔ 拋出 UnauthorizedException(401)                                 |
|       v                                                                                            |
|  [ 3. 業務服務層 ServiceImpl ]                                                                     |
|       | - 執行 @Transactional 業務邏輯、密碼校驗與資料持久化                                        |
|       v                                                                                            |
|  [ 4. 全域例外處理 GlobalExceptionHandler ]                                                        |
|       | - 攔截 UnauthorizedException ➔ 封裝為標準 ApiResponse(401, "請先登入系統")                  |
|       | - 攔截 MethodArgumentNotValidException ➔ 封裝為標準 ApiResponse(400, "第一筆欄位錯誤訊息") |
|       v                                                                                            |
|  [ 5. Filter finally 區塊 ]                                                                        |
|       | - 強制執行 UserContext.clear() 銷毀 ThreadLocal，徹底防禦執行緒池身分洩漏                   |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

> **關鍵架構決策**：  
> 未授權請求（無 Token、Token 過期或被竄改）**並非在 Filter 階段直接被中斷，而是放行進入 Controller 後，由 `UserContext.requireUserId()` 拋出 `UnauthorizedException`，最後由 `GlobalExceptionHandler` 統一轉譯為 HTTP 401 回應**。  
> `AuthIntegrationIT` 的斷言設計必須完整檢驗此一縱深防禦流向。

---

## 3. 基礎架構升級：集中式 Fixture 與快速身分工廠

### 3.1 為什麼必須採用集中式 Fixture？（效能與解耦考量）

在整合測試中準備已認證用戶身分時，若每個測試類別都透過 HTTP POST `/api/v1/auth/register` 與 `/login` 建立用戶，將引發兩大嚴重問題：
1. **CPU 效能瓶頸**：專案使用高安全強度 BCrypt 雜湊，每次運算耗時約 80ms。若全套整合測試有 40 個案例，將重複執行 80 次 BCrypt 密碼學計算，導致測試時間增加 6~8 秒。
2. **強烈脆弱性偶合**：若 Auth 模組修改註冊 DTO 或密碼規則，會導致無關的 `CategoryIT` 與 `LedgerIT` 全部假性報錯。

因此，本專案定案採用**集中於 `IntegrationTestBase` 提供記憶體 Fixture Helper 的途徑**：

```
+----------------------------------------------------------------------------------------------------+
|                                    兩種身分準備方式架構對比                                         |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ 途徑一：每個測試類別自己發起 HTTP 註冊/登入 (不推薦) ]                                          |
|    測試案例 A ──> POST /api/v1/auth/register (BCrypt 加密 ~80ms) ──> POST /login (BCrypt ~80ms)    |
|    - 缺點：40 個測試案例重複計算 BCrypt，套件執行時間膨脹 6~8 秒                                   |
|    - 缺點：緊密偶合，Auth API 格式調整波及所有業務模組測試                                         |
|                                                                                                    |
|  [ 途徑二：集中在 IntegrationTestBase 提供 Fixture Helper (本模組採用) ]                            |
|    測試案例 A ──┐                                                                                  |
|    測試案例 B ──┼─> createTestUser("alice")                                                        |
|    測試案例 C ──┘     ├─> userRepository.save(已準備好 BCrypt 假密碼)  <1ms                        |
|                       └─> tokenService.generateToken(userId, username) <1ms                        |
|    - 優點：完全跳過昂貴的 CPU 密碼運算，每個案例準備身分僅需 1 毫秒！                              |
|    - 優點：測試方法結束後，@Transactional 自動 ROLLBACK，乾淨無殘留                                 |
|    - 優點：關注點分離，後續 Category 與 Ledger 測試僅關注各自的核心領域邏輯                        |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

### 3.2 `IntegrationTestBase.java` 升級規格藍圖

```java
package com.tibame.integration.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibame.entity.User;
import com.tibame.repository.UserRepository;
import com.tibame.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 整合測試共用基底抽象類別 (升級版)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TokenService tokenService;

    // 事先計算好符合密碼政策之 BCrypt 雜湊 ("TestPass123!#")，跳過昂貴運算
    private static final String PRE_ENCRYPTED_PASSWORD_HASH =
            "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";

    public record TestUserContext(User user, String token) {
        public String bearerToken() {
            return "Bearer " + token;
        }
    }

    /**
     * 快速建立測試用戶並簽發有效 JWT Token (耗時 < 1ms)
     */
    protected TestUserContext createTestUser(String prefix) {
        String unique = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
        User user = User.builder()
                .username(unique)
                .passwordHash(PRE_ENCRYPTED_PASSWORD_HASH)
                .email(unique + "@example.com")
                .displayName("Test User " + unique)
                .build();
        User savedUser = userRepository.save(user);
        String token = tokenService.generateToken(savedUser.getId(), savedUser.getUsername());
        return new TestUserContext(savedUser, token);
    }

    protected String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}
```

---

## 4. `AuthIntegrationIT.java` 核心案例矩陣與規格契約

### 4.1 檢驗目標端點
* `POST /api/v1/auth/register`（用戶註冊）
* `POST /api/v1/auth/login`（用戶登入）
* `GET /api/v1/auth/me`（當前用戶資訊查詢）

### 4.2 涵蓋盲區
* **盲區 3**：Spring MVC Controller `@Valid` 參數校驗與 `MethodArgumentNotValidException` 轉譯格式。
* **盲區 4**：安全防護邊界（無 Token、偽造/竄改 Token、無 Bearer 前綴存取 `/me` 回傳 401）。

### 4.3 5 大黃金測試案例矩陣

| 序號 | 測試方法名稱 | 呼叫端點 | 請求條件 / Payload | 預期 HTTP 狀態碼 | 關鍵斷言 (Assertion) | 覆蓋意義 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **IT-AUTH-01** | `testRegisterPasswordPolicyViolation_Returns400` | `POST /api/v1/auth/register` | 密碼 `12345`（未滿 6 碼且無特殊字元） | `400 Bad Request` | `jsonPath("$.code").value(400)`<br>`jsonPath("$.message").value(containsString("密碼"))` | 驗證 `@Valid` 攔截與全域例外轉譯 |
| **IT-AUTH-02** | `testAccessMeWithoutToken_Returns401` | `GET /api/v1/auth/me` | 不附帶 `Authorization` Header | `401 Unauthorized` | `jsonPath("$.code").value(401)`<br>`jsonPath("$.message").value("請先登入系統")` | 驗證 `requireUserId()` 阻斷無 Token 請求 |
| **IT-AUTH-03** | `testAccessMeWithTamperedToken_Returns401` | `GET /api/v1/auth/me` | Header: `Bearer eyJhbGciOiJIUzI1Ni...偽造簽名` | `401 Unauthorized` | `jsonPath("$.code").value(401)` | 驗證 JWT 驗簽失敗後續 Controller 阻斷 |
| **IT-AUTH-04** | `testAccessMeWithInvalidBearerFormat_Returns401` | `GET /api/v1/auth/me` | Header: `Basic 123456`（缺少 Bearer 前綴） | `401 Unauthorized` | `jsonPath("$.code").value(401)` | 驗證非 Bearer 前綴無法注入身分並拋 401 |
| **IT-AUTH-05** | `testLoginSuccessAndFailureFlow` | `POST /api/v1/auth/login` | 1. 成功註冊後以正確密碼登入<br>2. 以錯誤密碼登入 | 1. `200 OK`<br>2. `401 Unauthorized` | 1. `jsonPath("$.data.token").isNotEmpty()`<br>2. `jsonPath("$.code").value(401)` | 驗證密碼比對邏輯與 Token 簽發整體時序 |

---

## 5. 程式碼實施藍圖 (`AuthIntegrationIT.java`)

```java
package com.tibame.integration;

import com.tibame.dto.LoginRequest;
import com.tibame.dto.RegisterRequest;
import com.tibame.integration.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("認證與安全邊界整合測試 (AuthIntegrationIT)")
class AuthIntegrationIT extends IntegrationTestBase {

    @Test
    @DisplayName("IT-AUTH-01: 註冊時密碼不符合安全政策應被 @Valid 攔截並回傳 400")
    void testRegisterPasswordPolicyViolation_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("weak_user");
        request.setPassword("12345"); // 未滿 6 碼且無特殊字元
        request.setEmail("weak@example.com");
        request.setDisplayName("Weak User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("密碼")));
    }

    @Test
    @DisplayName("IT-AUTH-02: 無 Authorization Header 存取 /me 應被阻斷並回傳 401")
    void testAccessMeWithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("請先登入系統"));
    }

    @Test
    @DisplayName("IT-AUTH-03: 使用偽造或竄改簽名之 Token 存取 /me 應回傳 401")
    void testAccessMeWithTamperedToken_Returns401() throws Exception {
        String tamperedToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("IT-AUTH-04: Authorization Header 缺少 Bearer 前綴應回傳 401")
    void testAccessMeWithInvalidBearerFormat_Returns401() throws Exception {
        TestUserContext testUser = createTestUser("bearer_test");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "InvalidPrefix " + testUser.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("IT-AUTH-05: 完整註冊與登入時序驗證 (正確回傳 200 與 Token，錯誤密碼回傳 401)")
    void testLoginSuccessAndFailureFlow() throws Exception {
        String username = "flow_user_" + System.currentTimeMillis();
        String rawPassword = "ValidPassword123!#";

        // 1. 成功註冊
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername(username);
        registerReq.setPassword(rawPassword);
        registerReq.setEmail(username + "@example.com");
        registerReq.setDisplayName("Flow User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()));

        // 2. 正確密碼登入 ➔ 200 OK
        LoginRequest validLogin = new LoginRequest();
        validLogin.setUsername(username);
        validLogin.setPassword(rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        // 3. 錯誤密碼登入 ➔ 401 Unauthorized
        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setUsername(username);
        invalidLogin.setPassword("WrongPassword999!#");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
```

---

## 6. 驗證指令與 Maven 生命週期調度

本套件可在 H2 記憶體模式與本機 MS SQL Server 測試資料庫（`tibame_account_test`）中無縫切換：

| 驗證情境 | 執行指令 | 預期執行時間 | 說明 |
| :--- | :--- | :--- | :--- |
| **快速本機驗證 (H2)** | `./mvnw test-compile failsafe:integration-test -Dit.test="AuthIntegrationIT"` | 約 1.5 ~ 2.0 秒 | 驗證 5 大案例邏輯與 Controller/Filter 整合 |
| **真實 MSSQL 測試** | `./mvnw test-compile failsafe:integration-test -Dspring.profiles.active=test-mssql -Dit.test="AuthIntegrationIT"` | 約 2.0 ~ 2.5 秒 | 直連 `tibame_account_test`，檢驗真實資料庫約束 |
| **全套門禁驗證** | `./mvnw verify` | 約 20 ~ 25 秒 | 包含 Surefire 單元測試、Failsafe 整合測試與 E2E 測試 |

---

## 7. OpenSpec 落地遷移規劃 (Actionable Next Steps)

完成探索後，可依照以下步驟發起正規 OpenSpec 變更並實作落地：

1. **發起變更提案**：
   ```bash
   openspec new change "add-auth-security-integration-tests"
   ```
2. **交付定義 (DoD) 檢核清單**：
   - [ ] `IntegrationTestBase.java` 成功加入 `createTestUser`、`toJson` 與相關相依注入。
   - [ ] `AuthIntegrationIT.java` 5 個測試案例全數建立並通過驗證。
   - [ ] 分別在預設 H2 與 `-Dspring.profiles.active=test-mssql` 下執行綠燈通過。
   - [ ] 測試執行完畢後交易自動回滾，無髒資料殘留。
   - [ ] 更新 `docs/README.md` 與測試手冊。
