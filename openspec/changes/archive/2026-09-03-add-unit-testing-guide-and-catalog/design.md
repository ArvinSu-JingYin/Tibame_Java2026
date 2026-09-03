## Context

參閱 `proposal.md`。目前專案已累積 54 個綠燈單元測試，涵蓋日常流水帳、分類管理、認證安全與通用密碼學模組。本設計旨在為開發團隊建立具備高維護性、易檢索且標準化的測試操作手冊與案例目錄，確立統一之測試架構規範。

## Goals / Non-Goals

**Goals:**
- 提供清晰完整的單元測試 CLI 與 IDE 操作手冊。
- 完整盤點全專案 54 個單元測試之方法名稱、情境與斷言驗證目標。
- 規範純單元測試（Pure Unit Testing）之零 Spring 容器依賴原則與 AAA (Arrange-Act-Assert) 撰寫樣板。
- 彙整核心業務安全防禦矩陣（多租戶水平越權 IDOR、系統內建分類唯讀保護、收支型別衝突防呆、Null 安全）。
- 提供後續新功能開發測試檢核清單 (Developer Checklist)。
- 將手冊完整整合進 `docs/` 知識庫與 `docs/daily_ledger_system/README.md` 目錄索引。

**Non-Goals:**
- 不更動現有生產環境代碼 (`src/main/java`)。
- 不更動已通過之測試程式碼邏輯（保持 54 個測試 100% 綠燈現狀）。
- 不引入額外之第三方測試報告插件（如 JaCoCo），保持依賴精簡。

## Decisions

1. **手冊檔案落點與導覽整合 (Documentation Placement)**：
   - **決策**：將手冊建立於 `docs/unit_testing_guide_and_test_catalog.md`，並在 `docs/daily_ledger_system/README.md` 補齊索引連結。
   - **理由**：現存測試不僅涵蓋記帳業務 (`daily-ledger-system`)，亦涵蓋共用資安模組 (`com.tibame.common.crypto`)。置於 `docs/` 根目錄符合跨模組通用性，同時透過 README 索引確保記帳模組開發者能迅速查閱。
   - **替代方案**：置於 `docs/daily_ledger_system/09_...`，但可能弱化密碼學模組的共用定位。

2. **54 個單元測試結構化表格化盤點 (Tabular Test Catalog)**：
   - **決策**：針對全量 54 個測試案例，以模組分組（業務服務層、密碼學/資安層、工具解析層）提供表格化對照清單。
   - **理由**：相較於純文字敘述，表格化能一目了然比對測試方法名稱與具體業務防線，利於 Code Review 與 QA 查驗。

3. **統一測試撰寫標準與檢核清單 (Standardized AAA & Checklist)**：
   - **決策**：明確定義 `@ExtendWith(MockitoExtension.class)` 零 Spring 上下文規範、繁體中文 `@DisplayName` 意圖說明，並列出 8 項新功能測試檢核項目。
   - **理由**：確保後續參與開發的工程師皆能維持相同的測試代碼品質與毫秒級執行效能。

## Risks / Trade-offs

- **[Risk] 後續新增功能時測試目錄文件脫節 (Doc Drift)**
  - *Mitigation*：在手冊最後章節建立「新功能開發測試檢核清單 (Developer Checklist)」，要求開發者在送出 PR/Commit 時一併檢核並維護該清單。
