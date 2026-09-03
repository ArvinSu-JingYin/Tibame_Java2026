## Context

參見 [proposal.md](proposal.md) 了解變更背景與動機。本專案先前於代碼層完成未使用引用清理、`JpaRepository` 多餘註解移除與正規表達式常數快取，並於工作區配置了 IDE 自動化規則。為使上述最佳實踐轉化為受 OpenSpec 生命週期納管的一級資產，需要定義規格層與設計層的架構方案。

## Goals / Non-Goals

**Goals:**
- 於 OpenSpec 建立 `engineering-standards` 能力規格，明確規範 IDE 工作區自動化、後端代碼潔淨慣例與 Zero-Warning DoD。
- 確立單一真實來源 (SSOT) 架構：以 `docs/specifications/engineering_standards_and_code_cleanliness.md` 作為完整技術與排除手冊，以 `openspec/specs/engineering-standards/spec.md` 作為可驗證的行為契約，以 `docs/README.md` 與各模組 `06_quality_assurance_and_dod.md` 作為鏈結索引。
- 規範 IDE 語言伺服器四步重置 SOP，解決跨平台開發時的假性紅字。

**Non-Goals:**
- 不變更任何既有業務領域邏輯、Controller 端點或資料庫綱要。
- 不引入新的 Maven 外部靜態檢查套件（如 Checkstyle / SpotBugs 插件），優先藉由 IDE 內建 Language Server 與 Maven 嚴格編譯達成零警告。

## Decisions

### 決策 1：雙軌文件與 OpenSpec 規格職責劃分
- **選擇**：
  - `openspec/specs/engineering-standards/spec.md`：專注於 SHALL/MUST 規範契約與測試/驗證場景（Scenarios）。
  - `docs/specifications/engineering_standards_and_code_cleanliness.md`：提供完整架構論述、代碼範例對照、Mermaid 流程圖與 IDE 排除指南。
- **替代方案考慮**：將全部教學內容直接寫入 OpenSpec spec.md。缺點是會違反 OpenSpec 簡潔行為契約原則，且不利於團隊快速查閱維護 SOP。

### 決策 2：IDE 自動化防護層級
- **選擇**：在 `.vscode/settings.json` 中配置 `"editor.codeActionsOnSave": { "source.organizeImports": "always" }` 與 `"boot-java.validation.java.version-validation": "OFF"`。
- **替代方案考慮**：要求開發者手動按快捷鍵清理 import。此方案依賴人工自律，容易在急迫提交時遺漏，自動化存檔觸發能從根源杜絕問題。

### 決策 3：零警告交付檢核矩陣 (Zero-Warning DoD)
- **選擇**：將「編譯成功、測試綠燈、0 未使用引用、0 孤兒死碼、0 多餘標註、0 Problems 面板警告」列為所有 OpenSpec 變更與 Git Commit 交付的共通閘門。
- **效益**：確保代碼庫持續處於極致整潔狀態，避免技術債累積。

## Risks / Trade-offs

- **[風險 1: 開發者使用不同 IDE（如 IntelliJ IDEA 或 Eclipse）可能無法套用 `.vscode/settings.json`]**  
  → **緩解措施**：於 `docs/specifications/engineering_standards_and_code_cleanliness.md` 詳述通用規範，並以 Maven 建置結果與 Zero-Warning DoD 作為最終驗收標準。
- **[風險 2: 重構正則表達式常數時殘留無用死碼欄位]**  
  → **緩解措施**：落實存檔自動檢查與 IDE Problems 面板清零原則，提交前務必確認零警告。
