## Context

本專案採用 Java 21、Spring Boot 3.3.x、Spring Data JPA 與 Maven 建置架構。在近期完成各模組單元測試（全套件 54 個測試情境全數通過）後，IDE（VS Code / Antigravity IDE）的 Problems（問題）面板仍出現多筆非阻塞性警告與雜訊（包含未使用引用、JpaRepository 多餘標註、正則表達式常數宣告與方法內重複編譯產生的死碼欄位，以及 Spring Tools 生命週期推播）。

技術細節與成因分析已於 [docs/code_cleanliness_and_ide_troubleshooting_exploration.md](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/docs/code_cleanliness_and_ide_troubleshooting_exploration.md) 完整記錄。詳細動機與改動範疇請參閱 [proposal.md](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/openspec/changes/code-cleanliness-and-ide-troubleshooting/proposal.md)。

## Goals / Non-Goals

**Goals:**
- 達成全專案 Problems 面板「零警告、零異味（Zero Warnings, Zero Code Smells）」標準。
- 遵循現代 Spring Data JPA 慣例，移除 `JpaRepository` 介面上多餘的 `@Repository` 標註。
- 落實單一真相原則（Single Source of Truth），將正則表達式提升為類別層級靜態常數快取（`private static final Pattern`），避免方法重複編譯與無用死碼欄位。
- 透過 `.vscode/settings.json` 自動化配置 `editor.codeActionsOnSave` 支援 `source.organizeImports: always`，並關閉 Spring Tools 生命週期過期推播。
- 固化規範至 `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md` 與 `openspec/config.yaml`，確立團隊與 AI 代理後續交付的 Definition of Done (DoD)。

**Non-Goals:**
- 不變更任何對外 RESTful Web API 規格、路徑、請求與回應結構。
- 不修改既有資料庫 Schema、JPA Entity 定義與事務控制邊界。
- 不進行 Spring Boot 大版本（如 3.5+ 或 4.x）之非相容性跨代升級。

## Decisions

### 決策 1：移除 JpaRepository 介面的 `@Repository` 標註
- **方案**：直接自 `UserRepository`、`CategoryRepository` 與 `AccountRecordRepository` 移除 `@Repository` 及其 `import`。
- **理由**：Spring Data JPA 於啟動時透過 `@EnableJpaRepositories` 自動為繼承 `JpaRepository` 的介面建立 `SimpleJpaRepository` 動態代理，且底層實作已內建資料庫例外轉譯（Exception Translation）。手動標註 `@Repository` 屬於冗餘語法，且會引發 IDE 擴充套件的 `Unnecessary @Repository` 警告。
- **替代方案評估**：保留標註無任何架構增益，反而持續造成認知噪音與 IDE 診斷面板堆疊。

### 決策 2：提升正則表達式為類別靜態常數快取（`private static final Pattern`）
- **方案**：將 `RegexSmartParserServiceImpl` 中的金額正則表達式統整為類別層級常數 `AMOUNT_PATTERN`，方法內僅呼叫 `AMOUNT_PATTERN.matcher(text)`。
- **理由**：`Pattern.compile(...)` 需構建有限狀態機（NFA/DFA），在請求方法內重複編譯會消耗 CPU 算力並造成 GC 壓力；同時先前方法內自定義區域 Pattern 導致類別層級 Pattern 成為死碼。此決策同時解決效能損耗與死碼警告。
- **替代方案評估**：方法內部快取（如 ThreadLocal 或本機 HashMap）徒增複雜度，Java 標準做法即為類別層級靜態編譯。

### 決策 3：IDE 存檔自動組織引用（Auto Organize Imports on Save）
- **方案**：在 `.vscode/settings.json` 中配置 `"[java]": { "editor.codeActionsOnSave": { "source.organizeImports": "always" } }`。
- **理由**：工程師重構時經常遺漏清理頂部 import，依靠手動快捷鍵（`Shift + Alt + O`）容易因疏失遺漏。存檔自動清理可確保所有 Java 原始碼在本地儲存時即維持 100% 零多餘 import。
- **替代方案評估**：僅依賴 CI/CD 檢查或 Git 預提交 Hook 會延後反饋，在 IDE 儲存當下自動處理體驗最佳。

### 決策 4：過濾 Spring Tools 生命週期推播並鎖定穩定修補版 3.3.13
- **方案**：於 `.vscode/settings.json` 加入 `"boot-java.validation.java.version-validation": "OFF"`，並維持 `pom.xml` 中 Spring Boot Starter Parent 為 `3.3.13`。
- **理由**：Spring Tools 預設會連網檢查 Spring Boot OSS 支援截止日，頻繁推播非阻塞性警告；關閉版本驗證通知可保持開發環境專注，同時採用 3.3.x 最終穩定修補版本 3.3.13 保障既有依賴與 API 穩定性。
- **替代方案評估**：全面升級 Spring Boot 3.5+ 會涉及潛在依賴衝突與設定變更，超出當前階段範疇。

## Risks / Trade-offs

- **[風險 1] 存檔自動清理 Import 導致意外移除註解相依類別** → **緩解措施**：JDT-LS 能精確識別各類型註解（包含 Lombok、Jakarta Validation 與 Spring 註解）之型別依賴，不會誤刪有效引用；且透過 Git Diff 可明確檢閱清理範圍。
- **[風險 2] 關閉版本驗證可能導致錯過未來重大安全性通知** → **緩解措施**：依賴定期排程之 Maven 依賴安全檢查工具（如 Dependabot 或 OWASP Dependency-Check），將版本管理權責明確移交給專案 CI 流程而非 IDE 即時推播。
