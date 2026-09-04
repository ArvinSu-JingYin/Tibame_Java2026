# 命名慣例、開發標準與檢核清單 (Coding Standards & DoD)

本文件定義 Spring Boot 專案之後端與前端命名規範、安全防護要求以及功能交付時必須通過的**開發檢核清單（Definition of Done, DoD）**。

---

## 1. 後端開發規範 (Backend Conventions)

### 1.1 Package 與目錄結構規範
- **套件名稱**：全小寫單數名詞，禁止駝峰或下劃線（如 `controller.api`, `controller.mvc`, `model.dto`, `model.vo`, `model.entity`）。
- **目錄職責嚴格隔離**：
  - `controller/mvc/`：僅放置使用 `@Controller` 之頁面視圖控制器。
  - `controller/api/`：僅放置使用 `@RestController` 之 RESTful Web API 控制器。
  - `repository/`：僅放置繼承 `JpaRepository` 或 MyBatis `@Mapper` 介面。
  - `service/`：放置業務介面；實作類必須位於 `service/impl/`。

### 1.2 類別命名規範
| 類型 | 命名格式 | 範例 |
| :--- | :--- | :--- |
| **MVC Controller** | `*ViewController` 或 `*Controller` | `UserViewController.java`, `DashboardController.java` |
| **Web API Controller** | `*ApiController` | `UserApiController.java`, `AuthApiController.java` |
| **Service 介面** | `*Service` | `UserService.java`, `OrderService.java` |
| **Service 實作類** | `*ServiceImpl` | `UserServiceImpl.java`, `OrderServiceImpl.java` |
| **Repository** | `*Repository` | `UserRepository.java`, `OrderRepository.java` |
| **Entity (實體)** | 單數名詞（大駝峰） | `User.java`, `Account.java` |
| **Request DTO** | `*CreateRequestDto` / `*UpdateRequestDto` / `*QueryDto` | `UserCreateRequestDto.java` |
| **Response VO** | `*ResponseVo` / `*SummaryVo` | `UserResponseVo.java`, `UserDetailVo.java` |

### 1.3 方法命名與事務邊界
- **查詢方法**：`get*ById`, `find*By*`, `query*`；務必標註 `@Transactional(readOnly = true)`。
- **異動方法**：`create*`, `update*`, `delete*`；涉及多步或金流交易必須標註 `@Transactional(rollbackFor = Exception.class)`。
- **輸入驗證**：Controller 接收 DTO 時必須加上 `@Valid` 或 `@Validated`。
- **Repository 標註規範**：繼承 `JpaRepository` 之介面嚴禁標註 `@Repository`（由 Spring Data 自動代理裝配，避免多餘標註）。
- **靜態正則與常數規範**：所有正則表達式（`Pattern`）必須以 `private static final` 宣告於類別層級快取，嚴禁在方法內重複 `Pattern.compile`；重構時嚴禁遺留未引用的死碼欄位。
- **單元測試修飾詞規範**：JUnit 5 測試類別與測試方法一律採套件預設可見性（不加 `public`）。

### 1.4 配置治理與注入邊界規範
- **禁止散裝 `@Value` 注入**：自訂配置屬性（如 `jwt.*`、`crypto.*` 等）一律封裝為獨立 `@ConfigurationProperties` 類別，**嚴禁**在 Controller、Service 或其他業務組件中散裝使用 `@Value("${...}")` 注入階層式屬性。
- **YAML Map 鍵名轉義**：YAML 設定檔中的 Map 鍵名若包含點號 `.` 或底線 `_`（如 `logging.level.*`、`properties.hibernate.*`），必須使用中括號轉義 `["..."]`。
- **元資料自動生成**：專案整合 `spring-boot-configuration-processor`，所有 `@ConfigurationProperties` 必須於編譯期產出元資料，供 IDE 智慧補全與類型驗證。

---

## 2. 前端開發規範 (Frontend Conventions)

### 2.1 檔案命名規範
- **CSS 樣式檔**：全部小寫烤肉串式（kebab-case），如 `swiss-theme.css`, `app.css`。
- **JS 腳本**：全部小寫烤肉串式（kebab-case），如 `http.js`, `alert.js`, `user-manage.js`。
- **Thymeleaf 模板**：全部小寫烤肉串式或目錄層級清晰（如 `pages/user/index.html`）。

### 2.2 Vue 3 狀態與方法命名
- **響應式狀態 (ref / reactive)**：小駝峰（lowerCamelCase），如 `userList`, `loading`, `currentUserId`。
- **事件處理函式**：`handle*` 或 `on*`，如 `handleSubmit`, `onFilterChange`。
- **非同步 API 呼叫方法**：`fetch*`, `save*`, `delete*`，如 `fetchUserList`, `saveUser`。

---

## 3. 安全性與校驗規範 (Security & Validation)

1. **參數輸入校驗**：
   - 所有的請求 DTO 欄位必須使用 `jakarta.validation.constraints.*` 明確定義規則（例如 `@NotBlank`, `@Email`, `@Size(min=6, max=20)`）。
2. **輸出遮蔽與脫敏**：
   - VO 類別嚴禁包含密碼哈希值（Password Hash）、Salt、信用卡號或內部敏感 Token。
3. **CSRF 防護**：
   - 前端發起 POST/PUT/DELETE 請求時，Axios 攔截器必須正確自 `<meta name="_csrf">` 標籤提取並附帶 CSRF Token。
4. **XSS 防禦**：
   - 視圖展示一律使用 Thymeleaf `th:text` 或 Vue `{{ ... }}` 插值；嚴禁在未經消毒情況下直接使用 `v-html` 或 `th:utext`。

---

## 4. 開發檢核清單 (Definition of Done, DoD)

在提交任何新功能或完成模組重構前，必須逐項核對並通過以下清單：

- [ ] **[後端] 四層架構分界**：MVC Controller 與 Web API Controller 邊界清晰，Controller 無直接注入 Repository。
- [ ] **[後端] 事務與異常規範**：Service 層事務邊界正確宣告，業務錯誤統一拋出自定義 `BusinessException`。
- [ ] **[後端] API 響應結構**：所有 Web API 一律返回標準 `ApiResponse<T>`，包含 `code`, `success`, `message`, `data`, `timestamp`。
- [ ] **[後端] 配置治理與強型別注入**：自訂屬性一律使用 `@ConfigurationProperties`，無散裝 `@Value` 注入；YAML Map 鍵名完成中括號轉義。
- [ ] **[前端] 嚴格離線 No-CDN**：頁面無任何外網 CDN 依賴，所有靜態庫本地化於 `resources/static/lib/`。
- [ ] **[前端] 瑞士風格視覺**：嚴格落實直角幾何邊框（0px 圓角）、無模糊陰影、經典瑞士紅輔助色、高對比無襯線排版。
- [ ] **[前端] 統一非同步與提示**：所有 API 呼叫使用封裝後的 `window.http`，成功/失敗互動統一調用 `window.SwissAlert`。
- [ ] **[品質] 自動化驗證測試守門**：`YamlConfigurationLintTest` 與 `ConfigurationMetadataTest` 100% 綠燈通過。
- [ ] **[品質] 代碼潔淨與零警告 (Zero-Warning DoD)**：
  - **編譯狀態**：執行 `.\mvnw.cmd clean test-compile` 確保 BUILD SUCCESS。
  - **單元測試**：執行 `.\mvnw.cmd test` 全套件測試 100% 綠燈通過。
  - **未使用引用**：全專案 0 未使用 Import（存檔自動組織引用）。
  - **無用私有欄位**：類別層級之 private 變數與常數皆有明確引用點，0 孤兒死碼。
  - **Problems 面板**：IDE 問題欄位無任何未解析之紅字 Error 與代碼風格 Warning。

