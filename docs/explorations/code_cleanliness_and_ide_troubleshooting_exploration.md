# 每日記帳系統 (Daily Ledger System) - 代碼潔淨、IDE 診斷與問題排除探索報告

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-03  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Java 21 / Spring Boot 3.3.13 / Spring Data JPA / Eclipse JDT-LS / Spring Tools 4  
> **目標範疇**：代碼潔淨規範、IDE 診斷診治、Spring Boot 工具雜訊過濾與 OpenSpec 規範固化  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與目標 (Background & Objectives)

在日常流水帳系統持續演進、引入完整單元測試與模組化重構的過程中，開發者於 IDE（VS Code / Antigravity IDE）下方的 **Problems（問題）面板** 常會觀察到多筆警告（Warnings）、代碼異味（Code Smells）與工具端推播通知（Tooling Notifications）。

這些問題雖然在底層 Maven 編譯時不會中斷（`mvnw test` 依然全數通過），但會造成以下負面影響：
1. **認知負載劇增**：混淆「真正的程式碼致命錯誤（Fatal Errors）」與「非阻塞警告（Warnings / Hints）」。
2. **潛在效能損耗**：方法內重複編譯正規表達式（`Pattern.compile`），產生不必要的 CPU 負載與垃圾回收（GC）壓力。
3. **架構慣例不一致**：Spring Data JPA 介面手動重複標註 `@Repository`，偏離現代框架最佳實踐。

本探索報告旨在系統性梳理問題成因，並提出自 **IDE 自動化**、**代碼重構慣例** 至 **OpenSpec 流程固化** 的全方位解決方案。

```
+-------------------------------------------------------------------------------------------------------+
|                                    IDE 診斷訊息與代碼異味分類矩陣                                      |
+-------------------------------------------------------------------------------------------------------+
| 類別                     | 代表現象與錯誤代碼                      | 影響層面      | 處置優先級               |
+--------------------------+-----------------------------------------+---------------+--------------------------+
| 1. 未使用引用 (Imports)  | The import ... is never used            | 代碼整潔度    | 優先處理（IDE 自動化）   |
| 2. 死碼與常數重複編譯    | The value of the field ... is not used  | CPU 效能與維護| 優先重構（提升靜態常數） |
| 3. 框架多餘標註          | Unnecessary @Repository                 | 架構慣例風格  | 移除（遵循現代規範）     |
| 4. 擴充套件生命週期雜訊  | BOOT_VERSION_VALIDATION_CODE            | 工具環境噪音  | 隔離（配置 OFF 關閉）    |
+--------------------------+-----------------------------------------+---------------+--------------------------+
```

---

## 2. 四大常見診斷問題深度剖析與實務案例

### 案例 1：未使用引用 (Unused Imports)

#### 根本成因
在重構業務邏輯或修訂測試情境時，刪除了特定的類別引用，但未同步清理檔案頂部的 `import` 陳述式。

#### 實務案例
- `AuthServiceTest.java`：初期撰寫重疊邏輯時引入了 `ConflictException`，但後續重構為只測 `UnauthorizedException`，導致引用孤立。
- `LedgerServiceTest.java`：引入了 `java.util.Collections` 與 `java.util.Set`，但測試實質上使用 Mockito 的 `anySet()` 靜態比對器，未直接用到原生物件。
- `TokenServiceTest.java`：宣告具體實作類別時，多餘引入了 `TokenService` 介面。

#### 解決與預防方案
1. **單檔手動清除**：在該檔案中按下快捷鍵 **`Shift + Alt + O`**（Windows）。
2. **自動化根治**：配置 [`.vscode/settings.json`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.vscode/settings.json)，實現每次 `Ctrl + S` 存檔時自動秒刪多餘 Import：
   ```json
   "[java]": {
     "editor.codeActionsOnSave": {
       "source.organizeImports": "always"
     }
   }
   ```

---

### 案例 2：無用私有常數與方法內正則重複編譯 (Unused Field & Pattern Overhead)

#### 根本成因
在 [`RegexSmartParserServiceImpl.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/service/impl/RegexSmartParserServiceImpl.java) 中，類別頂部宣告了：
```java
private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?<!\\d\\-)(?:\\$|NT\\$)?(\\d+(?:\\.\\d{1,2})?)(?!\\-\\d)");
```
然而在後續強化中文金額與日期排斥語意時，直接在 `parseQuickInput()` 方法內部撰寫：
```java
Pattern candidatePattern = Pattern.compile("(?<!\\d[-/])(?:\\$|NT\\$)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|塊)?(?![-/\\d]|\\s*月|\\s*日|\\s*號)");
Matcher amountMatcher = candidatePattern.matcher(text);
```
這直接導致類別層級的 `AMOUNT_PATTERN` 成為死碼，IDE 跳出 `The value of the field RegexSmartParserServiceImpl.AMOUNT_PATTERN is not used`。

#### 效能與品質損害
在 Java 中，`Pattern.compile(...)` 背後需建構非確定性有限狀態機 (NFA/DFA)，是一項 CPU 與記憶體開銷極高的操作。在方法內部呼叫 `compile` 會導致每次使用者請求解析流水帳時，皆重複消耗伺服器運算資源並產生 GC 壓力。

#### 重構解決方案
遵守**單一真相原則（Single Source of Truth）**，將優化後的正則表達式提升至類別層級快取：

```java
// 1. 類別常數層級（JVM 載入時僅編譯一次，高併發複用）
private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?<!\\d[-/])(?:\\$|NT\\$)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|塊)?(?![-/\\d]|\\s*月|\\s*日|\\s*號)"
);

// 2. 方法內部直接調用 matcher
@Override
public RecordCreateRequestDto parseQuickInput(String rawInput, Long userId) {
    ...
    Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
    while (amountMatcher.find()) { ... }
}
```

---

### 案例 3：Spring Data JPA 介面多餘標註 (Unnecessary `@Repository`)

#### 根本成因
在 [`UserRepository.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/repository/UserRepository.java)、[`CategoryRepository.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/repository/CategoryRepository.java) 與 [`AccountRecordRepository.java`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/repository/AccountRecordRepository.java) 介面上標註了 `@Repository`。

Spring Boot Tools 會拋出 `Unnecessary @Repository` 警告，原因如下：
1. **自動代理與裝配**：只要介面繼承自 `org.springframework.data.jpa.repository.JpaRepository`，Spring Boot 開箱即用的 `@EnableJpaRepositories` 自動掃描機制就會在執行期動態建立其代理實作（`SimpleJpaRepository`）。
2. **例外轉譯內建**：傳統 DAO 需要 `@Repository` 觸發 `PersistenceExceptionTranslationPostProcessor` 進行 SQL 例外轉譯，而 Spring Data 底層實作早已內建此功能。

#### 解決方案
遵循現代 Spring Boot 標準實踐，**直接自介面移除 `@Repository` 與其 import**：
```java
// 正確簡潔的現代 Spring Data JPA 介面定義
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

---

### 案例 4：擴充套件版本與生命週期噪聲 (Tooling Noise & Lifecycle Warnings)

#### 根本成因
IDE 內建的 Spring Boot Tools (`vmware.vscode-spring-boot`) 具備後台連線檢驗機制，會定期對照 Spring 官方端點比對專案的 `pom.xml`，拋出以下弱提示（Hint/Warning）：
- `Newer patch version of Spring Boot available: 3.3.13` (Severity: 4)
- `OSS support for Spring Boot 3.3.x ended on 2025-06-30` (Severity: 4)
- `Commercial support for Spring Boot 3.3.x ended on 2026-06-30` (Severity: 4)
- `Newer minor version of Spring Boot available: 3.5.16` (Severity: 2)

#### 解決方案（雙管齊下）
1. **升級當前主系列的最後修補版**：將 `pom.xml` 中 `spring-boot-starter-parent` 由 `3.3.3` 升級至最穩定的修補版 `3.3.13`，享受既有 API 相容性下的各項安全性修復。
2. **關閉擴充套件生命週期推播**：在 [`.vscode/settings.json`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.vscode/settings.json) 中加入：
   ```json
   "boot-java.validation.java.version-validation": "OFF"
   ```
   徹底杜絕 IDE 對過期生命週期的主動推播，避免干擾日常開發。

---

## 3. IDE 自動化防護配置最佳實踐

為確保團隊中每位工程師在本地編碼時享有完全一致且零干擾的環境，專案的 [`.vscode/settings.json`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.vscode/settings.json) 應收斂以下設定：

```json
{
  "git.inputValidation": true,
  "git.inputValidationLength": 72,
  "git.inputValidationSubjectLength": 72,
  "git.alwaysShowStagedChangesResourceGroup": true,
  "boot-java.validation.java.version-validation": "OFF",
  "[java]": {
    "editor.codeActionsOnSave": {
      "source.organizeImports": "always"
    }
  },
  "files.associations": {
    ".gitmessage": "markdown",
    "AGENTS.md": "markdown",
    "GEMINI.md": "markdown"
  }
}
```

### 常用 IDE 診斷命令備忘錄 (Command Palette: `Ctrl + Shift + P`)
- **重新載入專案**：`Java: Reload Projects`（繁體中文：`Java: 重新載入專案`）。
- **清理語言伺服器快取**：`Java: Clean Java Language Server Workspace`（繁體中文：`Java: 清理 Java 語言伺服器工作區`）。
- **重啟 IDE 視窗**：`Developer: Reload Window`（繁體中文：`開發人員: 重新載入視窗`）。

---

## 4. OpenSpec 開發規範與交付標準 (DoD Checkpoint)

為確保在使用 OpenSpec 進行工作流程（`/opsx-propose`, `/opsx-apply`, `/opsx-archive`）時，AI 代理與工程師皆產出零瑕疵代碼，已在規範庫中固化以下條文：

### 4.1 技能規範增補 (`spring-boot-skills`)
1. **Repository 介面原則**：繼承 `JpaRepository` 者一律不得配置 `@Repository`。
2. **正則常數原則**：所有 `Pattern` 必須以 `private static final` 宣告於類別層級，嚴禁於方法內部重複 `Pattern.compile`。
3. **單元測試修飾詞**：JUnit 5 測試類別與方法採套件預設可見性（不加 `public`）。

### 4.2 OpenSpec Apply 交付檢核標準 (Definition of Done)
在執行變更歸檔或提交 Git Commit 前，必須落實以下「零警告檢核」：

- [ ] **[編譯狀態]** 執行 `.\mvnw.cmd clean test-compile`，確保 BUILD SUCCESS。
- [ ] **[單元測試]** 執行 `.\mvnw.cmd test`，全套件測試 100% 綠燈通過。
- [ ] **[未使用引用]** 執行存檔自動整理或手動掃描，全專案 0 未使用 Import。
- [ ] **[無用私有欄位]** 類別層級之 private 變數與常數皆有明確引用點，0 孤兒死碼。
- [ ] **[Problems 面板]** 下方問題欄位無任何未解析之紅字 Error 與代碼風格 Warning。
