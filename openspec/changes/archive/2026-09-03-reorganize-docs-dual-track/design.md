## Context

參見 `proposal.md` 與 `docs/documentation_governance_and_dual_track_structure_exploration.md`。
目前專案之 `docs/` 資料夾內共有多份 Markdown 調研筆記與正式系統規範，所有檔案均平鋪於 `docs/` 根目錄或部分子目錄中，缺乏明確的探索 vs. 規範邊界以及頂層導覽中樞。

## Goals / Non-Goals

**Goals:**
- 建立標準雙軌目錄結構：`docs/explorations/`（探索軌）與 `docs/specifications/`（規範軌）。
- 依據探索報告收斂之分類矩陣，將既有檔案以保持 Git 歷史（`git mv`）方式遷移至目標目錄。
- 建立瑞士風格 (Swiss Style) 頂層門戶導覽地圖 `docs/README.md`，包含治理原則、文檔樹狀拓撲、跨主題檢索矩陣（Topic Matrix）與貢獻命名準則。
- 全面校驗並修復 Markdown 內部相對鏈結，確保文檔間交叉參照無死鏈。

**Non-Goals:**
- 不變更任何後端 Java 代碼、Spring Boot 配置或資料庫架構。
- 不大幅重寫或精簡既有探索文件的內文，維持其作為當時歷史脈絡與 Spike 調研記錄的完整性。
- 不變更 OpenSpec 本身的目錄結構（`openspec/`）。

## Decisions

### 決策 1：採用雙軌目錄制（Dual-Track Hierarchy）
- **選擇**：將文件嚴格分流為 `docs/explorations/` 與 `docs/specifications/`。
- **替代方案**：按業務功能拆分子目錄（如 `docs/auth/`、`docs/ledger/`、`docs/testing/`，且每個子目錄內各自混合調研與規範）。
- **理由**：按業務子目錄會使層級加深且模糊了「未定案的想法」與「已定案的契約」。雙軌制讓維運與開發人員能夠第一時間確定「我正在看的是已落地的真實規範，還是過往的思考草稿」。

### 決策 2：早期整合版大文件歸入探索軌封存
- **選擇**：將 `daily_ledger_system_specification_and_planning_report.md` 歸入 `docs/explorations/`。
- **替代方案**：刪除該文件，或置於 `docs/specifications/`。
- **理由**：該文件記錄了專案初期的整合藍圖，具有重要歷史脈絡價值；但正式規範已由 `docs/specifications/daily_ledger_system/`（01~09 拆分規格）接管。歸入探索軌既可避免與最新拆分規範衝突，又完整保留版本歷程。

### 決策 3：使用 `git mv` 進行文檔搬遷
- **選擇**：在實作 apply 階段使用 `git mv` 保留 Git blame 與歷史記錄。
- **替代方案**：直接複製檔案並刪除舊檔案。
- **理由**：文件是長期演進的資產，保留完整的提交歷史有利於後續追踪各章節的變更源頭。

### 決策 4：頂層導航中樞引入「跨維度主題檢索矩陣 (Topic Matrix)」
- **選擇**：在 `docs/README.md` 提供橫跨「身分認證」、「記帳業務」、「測試品質」、「維運腳本」的主題對照表，同時指向探索報告與權威規範。
- **理由**：解決雙軌制可能帶來的「同一領域文件分屬兩地不易一次查閱」問題，以表格索引作為立體導航中樞。

## Risks / Trade-offs

- **[Risk] 相對路徑斷鏈（Broken Relative Links）**
  - *影響*：搬遷後文件內的相對連結（如 `[02_functional](...)`）可能失效。
  - *緩解措施*：在搬遷後全面掃描並修正 Markdown 內的相對鏈結，並更新為對應的新路徑。
- **[Risk] 開發者習慣舊路徑**
  - *影響*：開發者可能依據記憶搜尋舊路徑。
  - *緩解措施*：頂層 `docs/README.md` 置於最醒目的位置，並在根目錄清楚指引雙軌路徑。

## Migration Plan

1. 建立目錄：`docs/explorations/` 與 `docs/specifications/`。
2. 檔案遷移：
   - 探索軌：遷移 6 份探索報告至 `docs/explorations/`。
   - 規範軌：遷移 `daily_ledger_system/`、`core_services_unit_testing_design.md`、`startup_script_specification_and_guide.md` 至 `docs/specifications/`。
3. 建立門戶：新建 `docs/README.md`。
4. 鏈結驗證：校對所有文件內部交叉參照路徑。
