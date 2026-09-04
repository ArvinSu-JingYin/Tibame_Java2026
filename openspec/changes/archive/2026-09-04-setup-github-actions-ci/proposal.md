## Why

隨著專案核心功能與端對端測試（Playwright E2E）趨於完善，目前本地測試套件包含 50+ 筆單元測試與 10+ 筆真機無頭瀏覽器測試。為確保多人協同開發與 AI 代理人提交時能夠即時攔截缺陷、維持規格一致性、保障提交歷史清晰可溯，亟需建立兼具「秒級快速門禁反饋」與「主幹深度驗收」的 GitHub Actions 持續整合（CI）管線架構。

此變更透過自動化 CI 門禁杜絕非合規 Commit 訊息、未通過 OpenSpec 驗證的變更，以及測試失敗代碼併入主分支，並藉由雙層快取策略大幅降低 CI 運行時間與資源消耗。

## What Changes

- **建立 PR 快速門禁工作流 (`.github/workflows/ci-pr.yml`)**：
  - `pr-compliance`：在 PR 建立、同步或編輯時，透過正規表達式嚴格校驗 PR 標題與分支內所有 Commit 訊息是否符合 Conventional Commits 格式、指定 Scope 清單與繁體中文簡述要求；並透過 Node.js 執行 `openspec validate --all` 確保規格合法性。
  - `pr-unit-test`：在 Java 21 環境啟用 Maven 依賴快取，以 H2 In-Memory 資料庫於 1 分鐘內極速執行 `mvn clean test`，排除耗時之 E2E 測試以達成 Fail-Fast。
- **建立主分支深度驗證工作流 (`.github/workflows/ci-main.yml`)**：
  - `main-verify`：在合併或 Push 至 `main` 分支時，啟用 Maven 快取與 Playwright 瀏覽器快取（`~/.cache/ms-playwright`），執行全量驗證 `mvn clean verify`（涵蓋單元測試、整合測試與 Playwright E2E 測試），並建置 Spring Boot 可執行 JAR。
  - 產出物與測試報告保存：測試完成後自動上傳 Surefire、Failsafe 報告與建置產物至 GitHub Artifacts（保留 7 天）。
- **建立專案 CI 與倉庫治理指引文檔 (`docs/guides/github-actions-ci-guide.md`)**：
  - 記錄 CI 雙層架構、本地除錯步驟、快取機制與 GitHub 分支保護規則（Branch Protection Rules）配置建議。

## Capabilities

### New Capabilities
- `ci-pipeline`: 定義 GitHub Actions 持續整合管線架構、PR 品質門禁守門（PR Title / Commit 訊息格式與繁體中文檢核、OpenSpec 規格驗證、極速單元測試）、Main 分支深度驗證（Playwright E2E 快取機制、全量測試與 Spring Boot 打包）及測試產物與報告歸檔規範。

### Modified Capabilities
<!-- 本次變更未修改既有規格之 requirement，因此無修改既有 capability -->

## Impact

- **CI/CD 管線**：新增 `.github/workflows/ci-pr.yml` 與 `.github/workflows/ci-main.yml` 兩個 GitHub Actions 工作流程。
- **治理文檔**：新增 `docs/guides/github-actions-ci-guide.md` 作為 CI 管線之操作與分支保護指南。
- **外部依賴與環境**：需於 GitHub Actions Runner 準備 Java 21 (Temurin)、Node.js 20 (執行 `@fission-ai/openspec`) 及 Playwright 系統依賴。
- **向後相容性**：不影響任何現有業務代碼、API 或測試邏輯，完全向後相容。
