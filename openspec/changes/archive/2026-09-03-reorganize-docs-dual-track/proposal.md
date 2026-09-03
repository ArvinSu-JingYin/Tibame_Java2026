## Why

隨著日常流水帳系統專案演進，`docs/` 目錄累積了大量架構調研、測試策略、密碼學探索與系統規格文件。當前所有文件皆處於扁平層級（Flat Hierarchy），導致開發者難以迅速區分「已定案的權威規範/SOP」與「探索階段的調研筆記/Spikes」，且早期整合大文件與後期模組化規格並存造成單一事實來源（SSOT）模糊，缺乏頂層門戶導航。

為了降低認知負載、釐清權威事實來源，專案需要正式導入「雙軌文件治理架構」（Dual-Track Structure），將文檔清晰劃分為探索軌（Exploration Track）與正式規範軌（Specification Track），並建立頂層導覽地圖。

## What Changes

- **建立雙軌目錄拓撲**：
  - 新增 `docs/explorations/` 目錄，收納技術選型、探索報告與歷史聚合大文件。
  - 新增 `docs/specifications/` 目錄，收納權威系統功能規格、架構設計、測試設計及維運指南。
- **文檔分類搬遷與重組**：
  - 將 6 份探索性質文檔（`system_exploration_report.md`、`modular_crypto_and_pqc_design_exploration.md`、`automated_testing_strategy_and_exploration.md`、`code_cleanliness_and_ide_troubleshooting_exploration.md`、`daily_ledger_system_specification_and_planning_report.md`、`documentation_governance_and_dual_track_structure_exploration.md`）遷移至 `docs/explorations/`。
  - 將核心業務拆分規格目錄 `daily_ledger_system/`（含 01~09 與模組 README）遷移至 `docs/specifications/daily_ledger_system/`。
  - 將 `core_services_unit_testing_design.md` 與 `startup_script_specification_and_guide.md` 遷移至 `docs/specifications/`。
- **新建頂層導覽門戶 (`docs/README.md`)**：
  - 提供專案全域文件導航地圖與瑞士極簡風格排版。
  - 闡述雙軌治理模型與文檔生命週期流轉機制。
  - 建立橫跨業務、認證、測試、維運等維度的主題檢索矩陣（Topic Matrix）。
  - 提供後續新文檔的新增規範與命名契約。
- **相對路徑與交叉參照鏈結校驗**：
  - 全面更新因目錄層級搬遷而受影響的 Markdown 檔案相對參照鏈結。

## Capabilities

### New Capabilities
- `documentation-governance`: 定義日常流水帳系統專案文件之雙軌目錄結構、頂層入口門戶標準、分類矩陣與生命週期流轉規範。

### Modified Capabilities
<!-- None -->

## Impact

- **文件系統與目錄**：`docs/` 根目錄將僅保留 `docs/README.md`，其餘文檔分別收納於 `docs/explorations/` 與 `docs/specifications/`。
- **專案源代碼與依賴**：完全無破壞性變更，不影響 Java 後端程式碼、Spring Boot 配置、資料庫 DDL 或 Maven 建置。
- **開發體驗與協同作業**：顯著提升新進與維護工程師檢索規範、操作手冊與探索調研的效率。
