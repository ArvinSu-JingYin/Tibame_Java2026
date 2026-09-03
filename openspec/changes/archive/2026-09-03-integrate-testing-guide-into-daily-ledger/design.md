## Context

參閱 `proposal.md`。日常流水帳系統 (`daily-ledger-system`) 已有 `01` 到 `08` 的系統性文件。本次變更透過目錄結構整併，將單元測試操作手冊與測試案例盤點清單正式編號為 `09`，收納至 `docs/daily_ledger_system/`。

## Goals / Non-Goals

**Goals:**
- 將單元測試手冊收納為 `docs/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md`。
- 統一頂部標題、Metadata 與編號體系，完全對齊 `01~08` 系列文件之瑞士風格與規範。
- 移除原 `docs/unit_testing_guide_and_test_catalog.md`，保持根目錄乾淨並維持單一事實來源。
- 更新 `docs/daily_ledger_system/README.md` 之導覽清單與鏈結。

**Non-Goals:**
- 不刪減 54 個單元測試之情境與盤點內容。
- 不更動任何業務生產程式碼 (`src/main/java`) 或測試程式碼 (`src/test/java`)。

## Decisions

1. **賦予編號 `09` (Sequential Numbering)**：
   - **決策**：檔名命名為 `09_unit_testing_guide_and_test_catalog.md`，主標題採用 `# 9. 單元測試操作手冊與測試案例盤點清單 (Unit Testing Guide & Test Catalog)`。
   - **理由**：接續 `08_system_operation_and_user_manual.md`，讓開發者在查閱操作手冊後，自然順延至單元測試與防禦矩陣，形成完美的工程規範閉環。

2. **徹底清理原檔案 (Clean Migration)**：
   - **決策**：移動後直接移除 `docs/unit_testing_guide_and_test_catalog.md`。
   - **理由**：杜絕專案中出現兩份相同手冊所產生的文件漂移 (Doc Drift) 風險。

## Risks / Trade-offs

- **[Risk] 相對路徑深度差異**
  - *Mitigation*：確認手冊內所標註之原始碼路徑為專案根目錄基準路徑（如 `src/test/java/...`），不受資料夾搬移影響；更新 `README.md` 的相對路徑為同目錄檔案參照。
