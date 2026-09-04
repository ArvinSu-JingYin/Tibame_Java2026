## Why

當前專案的 GitHub Actions CI 工作流程（`ci-main.yml` 與 `ci-pr.yml`）僅針對 `main` 分支設定觸發條件，且缺乏手動觸發機制（`workflow_dispatch`）。由於團隊日常開發主軌與遠端預設分支目前為 `dev`，開發人員將代碼推送到 `dev` 時 GitHub Actions 完全不會觸發任何 Workflow Runs。此外，若直接在 `ci-pr.yml` 啟用手動觸發，其內部 PR 標題校驗腳本會因缺少 PR 上下文而誤判失敗。為了確保日常開發能及時獲得 CI 品質守門，並賦予團隊手動觸發驗收與排查的彈性，亟需重構 CI 觸發矩陣並加入防護機制。

## What Changes

- **雙分支自動觸發支援**：
  - `ci-main.yml`：`push` 事件監聽分支由僅 `[main]` 擴展為 `[main, dev]`，確保推送到 `dev` 分支時亦能執行全量驗證與測試報表產出。
  - `ci-pr.yml`：`pull_request` 事件監聽分支由僅 `[main]` 擴展為 `[main, dev]`，確保發起至 `dev` 分支的 PR 同樣受到合規性與單元測試守門。
- **手動觸發機制 (`workflow_dispatch`) 整合**：
  - 為 `ci-main.yml` 與 `ci-pr.yml` 注入 `workflow_dispatch` 事件，支援從 GitHub Actions 介面或 CLI 手動派發工作流程。
- **PR 標題校驗相容性防禦**：
  - 重構 `ci-pr.yml` 的 `pr-compliance` 檢驗腳本，注入 `EVENT_NAME: ${{ github.event_name }}`。
  - 當事件類型非 `pull_request`（如 `workflow_dispatch` 手動觸發）時，自動略過 PR 標題非空與格式驗證，安全執行 OpenSpec 規格校驗、Commit 歷史校驗與後續單元測試。
- **治理文件與規格同步**：
  - 更新規格定義檔以規範雙分支與手動觸發行為。
  - 同步更新專案 CI 指南（`docs/guides/github-actions-ci-guide.md`）與全域文件導覽（`docs/README.md`）。

## Capabilities

### New Capabilities
<!-- 無新增 Capability -->

### Modified Capabilities
- `ci-pipeline`: 擴展 PR 快速守門與主幹深度驗收工作流程的觸發範疇（納入 `dev` 分支與 `workflow_dispatch` 手動派發），並定義手動觸發時 PR 標題檢查的防禦性相容行為。

## Impact

- **CI 工作流程檔案**：
  - `.github/workflows/ci-main.yml`：新增 `dev` push 觸發與 `workflow_dispatch`。
  - `.github/workflows/ci-pr.yml`：新增 `dev` PR 觸發、`workflow_dispatch` 以及 Node.js 標題驗證事件判斷邏輯。
- **規格與文件**：
  - `openspec/specs/ci-pipeline/spec.md`：更新 CI 觸發規格。
  - `docs/guides/github-actions-ci-guide.md`：更新觸發矩陣與手動執行指南。
- **開發體驗與安全性**：
  - 推送 `dev` 分支即可即時獲得自動化測試反饋，排查與部署具備隨選手動觸發能力，且無破壞性變更。
