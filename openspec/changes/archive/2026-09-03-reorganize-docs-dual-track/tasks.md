## 1. Directory Structure Initialization & Document Migration

- [x] 1.1 建立 `docs/explorations/` 與 `docs/specifications/` 目錄結構，並透過目錄檢查指令確認目錄就緒
- [x] 1.2 使用 `git mv` 將 6 份探索文件（`system_exploration_report.md`、`modular_crypto_and_pqc_design_exploration.md`、`automated_testing_strategy_and_exploration.md`、`code_cleanliness_and_ide_troubleshooting_exploration.md`、`daily_ledger_system_specification_and_planning_report.md`、`documentation_governance_and_dual_track_structure_exploration.md`）遷移至 `docs/explorations/`，並以 `git status` 驗證搬遷與歷史保留
- [x] 1.3 使用 `git mv` 將規格模組目錄 `daily_ledger_system/` 遷移至 `docs/specifications/daily_ledger_system/`，並將 `core_services_unit_testing_design.md` 與 `startup_script_specification_and_guide.md` 遷移至 `docs/specifications/`，驗證目標檔案存在

## 2. Top-Level Documentation Portal Creation

- [x] 2.1 建立頂層導航門戶 `docs/README.md`，撰寫雙軌架構定位、目錄拓撲、跨領域主題矩陣（Topic Matrix）與文件貢獻規範，並確認內容與鏈結就緒
- [x] 2.2 檢視並更新 `docs/specifications/daily_ledger_system/README.md` 及相關規範中的返回頂層門戶相對路徑

## 3. Link Verification and Integrity Audit

- [x] 3.1 全面掃描檢查 `docs/` 內所有 Markdown 文件的相對參照鏈結，修正因目錄層級改變而受影響的路徑，確保無死鏈
- [x] 3.2 驗證 `docs/` 根目錄除 `README.md` 外無任何散落的 Markdown 檔案，確認雙軌架構乾淨符合規範
