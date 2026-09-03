# 專案通用工程標準、代碼潔淨與 IDE 排除指南 (Engineering Standards & Code Cleanliness)

> **文件版本**：v1.0.0  
> **制定日期**：2026-09-03  
> **規範層級**：專案全域權威規範 (Repository-Wide Specification)  
> **適用範圍**：所有後端 Java / Spring Boot 模組、前端靜態資產與 IDE 本地環境  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **探索溯源**：[探索報告：代碼潔淨、IDE 診斷與問題排除 (code_cleanliness_and_ide_troubleshooting_exploration.md)](../explorations/code_cleanliness_and_ide_troubleshooting_exploration.md)

---

## 1. 概述與設計宗旨 (Overview & Philosophy)

本文件定義專案全域通用的工程標準、代碼整潔度慣例、IDE 工作區自動化防護機制與交付檢核清單（Zero-Warning DoD）。

### 核心目標
1. **單一真實來源 (Single Source of Truth, SSOT)**：統一全專案的代碼潔淨度與架構慣例，避免各子系統或業務模組重複定義或產生規則歧異。
2. **零認知負載 (Zero Cognitive Load)**：透過自動化工具過濾非關鍵環境警告，讓工程師聚焦於業務邏輯與架構演進。
3. **極致執行效能與乾淨度 (High Performance & Clean Code)**：杜絕高開銷物件（如正規表達式 `Pattern.compile`）之運行期重複編譯，消除死碼欄位。
4. **全套件零警告交付 (Zero-Warning Delivery)**：在功能交付、代碼審查（Code Review）與 CI/CD 建置前，落實嚴格的「零警告」檢驗。

---

## 2. IDE 工作區自動化標準 (IDE Workspace Automation)

為確保跨作業系統（Windows / Linux / macOS）及各 IDE（VS Code / Antigravity IDE）擁有一致且無干擾的開發體驗，專案根目錄的 [`.vscode/settings.json`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.vscode/settings.json) 必須強制遵循以下自動化配置：

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

### 2.1 存檔自動組織引用 (`source.organizeImports: always`)
- **機制**：每次按下 `Ctrl + S` 儲存 Java 檔案時，IDE 自動執行未引用類別清理與字母排序。
- **效益**：從源頭徹底根除 `The import ... is never used` 警告，避免手動呼叫 `Shift + Alt + O` 的疏漏。

### 2.2 關閉擴充套件版本弱提示 (`version-validation: OFF`)
- **機制**：關閉 Spring Boot Tools 後台主動對照 Spring 官方端點生命週期所拋出的非阻塞弱提示（如 `OSS support for Spring Boot 3.3.x ended on...`）。
- **效益**：杜絕 IDE 下方 Problems 面板的工具噪音，確保問題欄位僅呈現真正的代碼錯誤與架構警告。

---

## 3. 後端代碼潔淨與架構慣例 (Backend Conventions)

### 3.1 Spring Data JPA 介面標註原則
- **規則**：凡繼承自 `org.springframework.data.jpa.repository.JpaRepository` 之資料庫存取介面，**嚴禁標註 `@Repository` 註解**。
- **成因與效益**：
  1. Spring Boot 的 `@EnableJpaRepositories` 自動掃描機制會在執行期動態建立其代理實作（`SimpleJpaRepository`）。
  2. Spring Data JPA 代理層早已內建 SQL 例外轉譯機制（Exception Translation），手動標註為重複多餘，且會引發現代語言伺服器拋出 `Unnecessary @Repository` 警告。

```java
// 正確：簡潔、現代化的 Spring Data JPA 介面
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

### 3.2 正規表達式編譯與常數快取原則
- **規則**：所有正則表達式（`java.util.regex.Pattern`）**必須以 `private static final` 宣告於類別常數層級快取**，嚴禁在業務方法內部重複調用 `Pattern.compile(...)`。
- **成因與效益**：
  1. `Pattern.compile` 會進行正規語法詞法分析與 NFA/DFA 狀態機建構，屬 CPU 密集且產生大量暫時性物件的高成本運算。
  2. 提升至類別靜態常數可保證在 JVM 載入時僅編譯一次，於高併發請求下具備執行緒安全性（Thread-safe）且零額外記憶體開銷。
  3. 重構時必須落實**單一真相原則**，嚴禁保留已無人引用的孤兒死碼常數（避免 `The value of the field ... is not used`）。

```java
// 正確：類別常數快取，方法內直接複用
public class RegexSmartParserServiceImpl implements SmartParserService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d[-/])(?:\\$|NT\\$)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|塊)?(?![-/\\d]|\\s*月|\\s*日|\\s*號)"
    );

    @Override
    public RecordCreateRequestDto parseQuickInput(String rawInput, Long userId) {
        ...
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        while (amountMatcher.find()) {
            ...
        }
    }
}
```

### 3.3 JUnit 5 測試類別修飾詞原則
- **規則**：JUnit 5 (Jupiter) 測試類別與測試方法一律採用**套件預設可見性（Package-private，不加 `public` 修飾詞）**。
- **效益**：符合 JUnit 5 官方現代最佳實踐，減少不必要的代碼冗贅，避免反射封裝洩漏。

```java
// 正確：不加 public 修飾詞
class AuthServiceTest {

    @Test
    void register_withValidDto_shouldReturnUserResponseVo() {
        ...
    }
}
```

---

## 4. IDE 語言伺服器故障排除 SOP (Troubleshooting Manual)

當 IDE 的 Problems 面板出現假性紅字（Phantom Errors）、語法高亮失效或專案建置快取不一致時，依序執行以下排除步驟（於 VS Code / Antigravity IDE 中按下 `Ctrl + Shift + P` 開啟命令選擇區）：

```
+-----------------------------------------------------------------------------------------------+
|                             IDE 語言伺服器自我修復四步工作流                                    |
+-----------------------------------------------------------------------------------------------+

  [步驟 1: 重新載入專案]
  Java: Reload Projects (繁中：Java: 重新載入專案)
        | (若仍有假性報錯)
        v
  [步驟 2: 清除語系快取]
  Java: Clean Java Language Server Workspace (繁中：Java: 清理 Java 語言伺服器工作區)
        | (勾選 Restart and Delete)
        v
  [步驟 3: 重啟 IDE 視窗]
  Developer: Reload Window (繁中：開發人員: 重新載入視窗)
        | (若底層依賴未下載完全)
        v
  [步驟 4: 終端強制重新編譯]
  執行 .\mvnw.cmd clean test-compile -U
```

---

## 5. 專案零警告交付檢核矩陣 (Zero-Warning DoD)

全專案所有功能模組於提交 Git Commit、建立 Pull Request 或執行 OpenSpec 歸檔 (`/opsx-archive`) 前，必須逐項核對並滿足以下品質門檻：

```mermaid
checklist
    title 零警告品質交付矩陣 (Zero-Warning DoD)
    - [x] 編譯狀態：執行 .\mvnw.cmd clean test-compile 確保 BUILD SUCCESS
    - [x] 單元測試：執行 .\mvnw.cmd test 全套件測試 100% 綠燈通過
    - [x] 未使用引用：全專案 0 未使用 Import (由存檔自動組織保障)
    - [x] 孤兒死碼：類別層級私有欄位皆有明確引用點，0 未使用常數/變數
    - [x] 介面標註：JpaRepository 介面無多餘 @Repository 標註
    - [x] 正則快取：所有 Pattern 均已宣告為 private static final 類別常數
    - [x] Problems 面板：IDE 問題面板維持 0 錯誤、0 警告 (Zero-Warning)
```

---

## 6. 與各業務模組之整合指引 (Integration with Modules)

任何業務子系統（例如 `daily_ledger_system`）在其專屬的品質驗收規格（如 [06_quality_assurance_and_dod.md](daily_ledger_system/06_quality_assurance_and_dod.md)）中，無須重複撰寫上述細部技術條例，僅需聲明遵循本規範：

```markdown
- [x] 代碼潔淨零警告：通過 [專案通用工程標準、代碼潔淨與 IDE 排除指南](../engineering_standards_and_code_cleanliness.md)
```
此舉既能維護全專案單一真實來源（SSOT），又能賦予各業務模組高度的驗收約束力。
