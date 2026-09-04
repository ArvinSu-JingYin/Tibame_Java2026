## Context

專案已建立 GitHub Actions CI 雙層架構：`ci-pr.yml`（PR 快速守門）與 `ci-main.yml`（主幹深度驗收）。然而目前兩者僅監聽 `main` 分支事件，且缺少 `workflow_dispatch` 手動觸發設定。當前專案日常開發主軌與遠端預設分支均為 `dev`，造成推送代碼後無法觸發 CI。詳見 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- 支援 `dev` 與 `main` 雙分支的自動 CI 觸發矩陣。
- 支援 GitHub Actions 介面與 CLI 的 `workflow_dispatch` 手動觸發。
- 在 `ci-pr.yml` 中實現事件類型感知（`github.event_name`），在手動觸發時優雅跳過 PR 標題校驗，避免誤判中斷。
- 維持現有繁體中文 Conventional Commits 格式檢驗、OpenSpec 規格驗證、單元測試快速守門與主幹 Playwright 深度驗證的完整性。
- 同步更新 CI 治理指南（`docs/guides/github-actions-ci-guide.md`）與全域文件索引（`docs/README.md`）。

**Non-Goals:**
- 不變更現有 Spring Boot 後端業務代碼、資料庫配置或 Vue 3 前端資源。
- 不調整 Maven 依賴或 Playwright 測試用例。
- 不引入額外的第三方 GitHub Actions 外掛程式或 CD 發布流程。

## Decisions

### 1. 雙軌分支監聽策略 (Dual-Branch Trigger Strategy)
- **決策**：在 `ci-main.yml` 的 `push.branches` 與 `ci-pr.yml` 的 `pull_request.branches` 中統一配置 `[main, dev]`。
- **理由**：團隊日常開發於 `dev` 分支迭代，推送到 `dev` 時即可獲得即時的全量測試與 JAR 建置驗證；同時未來發起合併至 `dev` 或 `main` 的 PR 均可享有快速合規守門。
- **替代方案評估**：
  - *僅改為 dev*：未來合併至 `main` 時將失去 CI 保護，不可行。
  - *維持僅 main 並要求團隊切換至 main*：與團隊目前的多分支工作流程衝突，增加協作摩擦。

### 2. 注入 `workflow_dispatch` 手動派發
- **決策**：在兩個 Workflow 的 `on:` 定義中宣告 `workflow_dispatch:`。
- **理由**：提供開發者在 Actions 介面點擊「Run workflow」隨時進行健康診斷與除錯之能力，不受特定 Git 事件約束。

### 3. PR 標題檢驗腳本的事件環境變數防禦
- **決策**：在 `ci-pr.yml` 的 `pr-compliance` 步驟中，注入 `EVENT_NAME: ${{ github.event_name }}`，並在 Node.js 檢驗邏輯中加入條件分支判斷：
  ```javascript
  const eventName = process.env.EVENT_NAME || "";
  if (eventName === "pull_request") {
    console.log("=== 正在校驗 PR 標題 ===");
    validate("PR Title", prTitle);
  } else {
    console.log("非 PR 事件（如手動觸發 workflow_dispatch），略過 PR 標題校驗。");
  }
  ```
- **理由**：手動觸發時 `github.event.pull_request` 為空，`PR_TITLE` 為空字串。若不進行事件判斷，原腳本會直接判定失敗（Exit Code 1）。透過事件感知，手動觸發時能順利執行 OpenSpec 規格驗證、Commit 歷史驗證與單元測試。

### 4. Git 提交歷史提取之容錯回退增強
- **決策**：在手動觸發或 `BASE_REF` 為空的情況下，維持現有 `try/catch` 容錯邏輯，預設 `baseRef` 為 `"main"`，若 `origin/${baseRef}..HEAD` 失敗則回退至 `git log -1`，確保手動觸發時 Commit 校驗不發生未捕獲異常。

## Risks / Trade-offs

- **[Risk] 雙分支觸發導致 GitHub Actions 運行頻率增加**  
  → *Mitigation*：`ci-pr.yml` 採用極速輕量測試（略過 Playwright，耗時 < 1 分鐘）；`ci-main.yml` 配備 Maven 與 Playwright 瀏覽器快取機制，大幅降低重複下載開銷。
- **[Risk] 手動觸發 `ci-pr.yml` 略過 PR 標題檢查可能導致不合規 PR 僥倖通過**  
  → *Mitigation*：GitHub 上的 Branch Protection Rule 強制要求 PR 上的實際 `pull_request` 檢查通過，手動觸發僅用於診斷，且日誌會明確打印「略過 PR 標題校驗」之審計紀錄。

## Migration Plan

1. **工作流程更新**：編輯 `.github/workflows/ci-main.yml` 與 `.github/workflows/ci-pr.yml`。
2. **規格檢驗**：執行 `openspec validate --all` 確保全域規格語法合規。
3. **治理文件同步**：更新 `docs/guides/github-actions-ci-guide.md` 與 `docs/README.md`。
4. **驗證與上線**：提交並推送變更至遠端 `dev` 分支，確認 GitHub Actions 即時啟動 Workflow Run，並驗證「Run workflow」按鈕可正常手動調度。
