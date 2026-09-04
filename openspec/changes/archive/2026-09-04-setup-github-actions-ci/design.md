## Context

參見 [proposal.md](proposal.md)。
專案現況為 Java 21、Spring Boot 3.3.13、Maven 建置系統，採用 Playwright 1.46.0 進行 E2E 測試，並使用 OpenSpec CLI (`@fission-ai/openspec`) 進行規格驅動開發。本地測試已劃分單元測試（Surefire）與 E2E 整合測試（Failsafe），但缺乏雲端持續整合（CI）自動化守門與提交規範強制校驗機制。

## Goals / Non-Goals

**Goals:**
- 建立雙層 CI 管線：PR 階段極速門禁（Fail Fast，< 1 分鐘）與 Main 分支深度整合（Deep Integration）。
- 機械化阻擋非合規 Commit 訊息與 PR 標題（強制 Conventional Commits、專案專屬 Scope 白名單與繁體中文簡述）。
- 自動化 OpenSpec 規格一致性檢驗（`openspec validate --all`），防止破損或未落實的規格合流。
- 建立 Playwright 瀏覽器快取機制（`~/.cache/ms-playwright`），縮短 E2E 執行時間。
- 收集並上傳 Surefire/Failsafe 測試報告與 Spring Boot 可執行 JAR 至 GitHub Artifacts（保留 7 天）。
- 提供專案分支保護規則（Branch Protection Rules）配置指南與治理文檔。

**Non-Goals:**
- 自動化持續部署（CD）或發布至生產環境伺服器（不在本次 CI 範疇）。
- 於 PR 階段啟動 Playwright 真機瀏覽器（避免拖慢 PR 審查與反饋效率）。
- 連接外部實體 MS SQL Server 資料庫（全量測試均以 H2 In-Memory `MODE=MSSQLServer` 進行）。

## Decisions

### 1. 雙層分流管線架構 (Two-Tier Pipeline Architecture)
- **決策**：劃分 `.github/workflows/ci-pr.yml`（PR 門禁）與 `.github/workflows/ci-main.yml`（主幹深度驗證）。
- **理由**：開發者在發起 PR 時需要秒級反饋（快速確認單元測試、代碼合規與規格有效性）；而在代碼合流至 main 主幹時，再執行耗時較長的 Playwright E2E 測試與 BootJar 封裝，達成開發效率與品質保證的最佳平衡。
- **替代方案**：單一 CI 工作流（所有事件均跑全量測試）——缺點是每次小修 PR 都需耗費 3~5 分鐘等待無頭瀏覽器，嚴重阻礙開發流暢度。

### 2. 輕量化 Bash 正則校驗 PR 標題與 Commit 訊息
- **決策**：在 `ci-pr.yml` 的 `pr-compliance` 任務中，使用原生 Bash 與正規表達式檢驗 `github.event.pull_request.title` 及分支提交歷史。
  - 正則表示式：`^(feat|fix|refactor|perf|test|style|docs|chore|revert)\((controller|service|repository|entity|dto|config|security|exception|view|common|build|specs)\): .*[一-龥].*`
  - 結尾禁止帶句號。
- **理由**：不需額外引入重型的 `commitlint` 或 Node.js 外掛設定檔，極簡且完全契合專案現行全域規範。
- **替代方案**：引入 npm `@commitlint/cli` 與自訂外掛——設定複雜且增加維護負擔。

### 3. Playwright 瀏覽器快取策略 (`actions/cache@v4`)
- **決策**：在 `ci-main.yml` 中快取 `~/.cache/ms-playwright` 目錄，快取鍵值以 `runner.os` 及 `pom.xml` 的 hash 作為識別；若未命中才執行 `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"`。
- **理由**：Playwright 瀏覽器二進位檔約 100~150MB，且跨運行需重新下載；快取能節省 1~2 分鐘下載時間與頻寬。
- **替代方案**：每次執行皆完整重新下載安裝——浪費 GitHub Actions runner 時間。

### 4. OpenSpec CLI 自動化檢驗
- **決策**：在 `ci-pr.yml` 中配置 Node.js 20，安裝全域 `@fission-ai/openspec`，並執行 `openspec validate --all`。
- **理由**：即時在 CI 攔截格式錯誤、漏填欄位或 Task 未標註完成的規格變更，杜絕規格腐化。

## Risks / Trade-offs

- **[Risk] Linux 環境下 Playwright 依賴庫缺漏**  
  → *Mitigation*：Playwright 於快取未命中時呼叫 `install --with-deps chromium`，自動安裝所需的 Ubuntu 系統 `.so` 套件。
- **[Risk] 本地 Windows (mvnw.cmd) 與 CI Linux (mvnw) 換行符或執行權限問題**  
  → *Mitigation*：在 CI Step 執行前確保 `./mvnw` 具備可執行權限（`chmod +x ./mvnw`），並透過 Git 確保 `.mvn` Wrapper 檔案權限正確。
- **[Risk] 正則表達式中文比對相容性**  
  → *Mitigation*：在 bash 中比對字元範圍 `[一-龥]` 或使用 Python/Perl 腳本做 Unicode 嚴格比對，確保 GitHub Actions Ubuntu Runner 能正確識別繁簡漢字。

## Migration Plan

1. 建立 `.github/workflows/ci-pr.yml` 與 `.github/workflows/ci-main.yml`。
2. 撰寫專案 CI 指引與分支保護說明文檔 `docs/guides/github-actions-ci-guide.md`。
3. 將工作流提交至倉庫，並於 GitHub 專案後台配置 Branch Protection Rules。
