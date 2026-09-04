## Context

關於問題成因與動機，請參閱 `proposal.md`。
現行 GitHub Actions Runner (Ubuntu Latest) 將執行期強轉為 Node.js 24，導致使用 `node20` 的 actions 與已宣告 EOL 的 `actions/setup-java@v4` 頻繁輸出警告。專案需在不影響現有 Java 21、Maven 快取、Playwright E2E 與 Commit 繁中校驗門禁的前提下完成現代化升級。

## Goals / Non-Goals

**Goals:**
- 消除 GitHub Actions 執行日誌中關於 `setup-java v4 is deprecated` 與 `node20 forced to run on Node.js 24` 之警示。
- 升級 `.github/workflows/ci-main.yml` 與 `.github/workflows/ci-pr.yml` 中之 `actions/setup-java` 至 `@v5`，並維持 Temurin JDK 21 與 Maven 快取機制。
- 將 `ci-pr.yml` 門禁中的 Node.js 執行環境升級至 `node-version: 22` (Active LTS)。
- 維持規格書 `openspec/specs/ci-pipeline/spec.md` 與開發手冊 `docs/guides/github-actions-ci-guide.md` 與 CI 實際行為一致。

**Non-Goals:**
- 不變更 Java 21、Spring Boot 3.x 或 Maven 專案相依版本。
- 不調整 Playwright Chromium 瀏覽器快取路徑與 E2E 測試流程。
- 不修改 PR 標題與 Commit 訊息之 Conventional Commits 繁體中文格式規範。
- 不更動尚無 `@v5` 發布之官方 Action（如 `actions/checkout@v4`、`actions/cache@v4`、`actions/upload-artifact@v4`）。

## Decisions

### 決策一：升級 `actions/setup-java` 為 `@v5`
- **選擇**：將 `ci-main.yml` 與 `ci-pr.yml` 中所有 `actions/setup-java@v4` 替換為 `actions/setup-java@v5`。
- **理由**：
  - `@v5` 為官方最新發行版，原生採用 Node.js 24 執行期架構。
  - 參數定義（`java-version: '21'`、`distribution: 'temurin'`、`cache: 'maven'`）完全向下相容，屬於無損替換 (Drop-in Replacement)。
- **備選方案**：保留 `@v4`。缺點為技術債積累，且不再接收官方安全性修復與維護。

### 決策二：PR 門禁選用 Node.js 22 LTS (Active LTS)
- **選擇**：將 `ci-pr.yml` 中 `pr-compliance` 任務之 `node-version` 宣告為 `22`。
- **理由**：
  - Node.js 22（代號 Jod）為當前官方 Active LTS，具備極高的企業級穩定度與廣泛生態相容性。
  - 完美相容 `@fission-ai/openspec` CLI 與內聯 Node.js 正則校驗腳本。
  - 消除 Runner 淘汰警告。
- **備選方案**：
  - 選用 Node.js 24 (Current)：雖然與底層對齊，但尚非 LTS，工具鏈偶有非預期 breaking changes 風險。
  - 維持 Node.js 20：處於 Maintenance LTS 且即將結束生命週期。

### 決策三：保持其他 Action 版本為 `@v4`
- **選擇**：維持 `actions/checkout@v4`、`actions/cache@v4`、`actions/upload-artifact@v4`。
- **理由**：GitHub 官方尚未釋出這些 actions 的 `@v5` 正式版本；Runner 目前提供透明相容執行，後續待官方發布新大版本時再獨立規劃升級。

## Risks / Trade-offs

- **[Risk]** `@fission-ai/openspec` 工具鏈在 Node.js 22 環境下相容性問題  
  → **[Mitigation]** 本地與 CI 門禁實測驗證 `openspec validate --all`，確認語法解析與 schema 驗證 100% 正常。
- **[Risk]** `actions/setup-java@v5` 在 Maven 快取或 Linux 權限方面之微小行為變化  
  → **[Mitigation]** 保留現有 `chmod +x ./mvnw` 步驟，並在 CI 驗證快取命中與建置日誌。
