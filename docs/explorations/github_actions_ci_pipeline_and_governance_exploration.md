# GitHub Actions CI 管線架構、品質守門與自動化治理探索報告 (GitHub Actions CI Pipeline & Governance Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：GitHub Actions / Java 21 / Spring Boot 3.3.13 / Maven / Playwright / H2 In-Memory DB / OpenSpec CLI  
> **目標範疇**：CI 雙層分流管線設計、Commit 規範與繁中強制檢驗、OpenSpec 規格自動驗證、Playwright E2E 快取最佳實踐、分支保護策略  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與核心目標 (Background & Objectives)

本專案（每日記帳系統 Daily Ledger System）採用高內聚之後端四層架構、瑞士風格（Swiss Style）離線前端、Playwright E2E 自動化測試與 OpenSpec 規格驅動開發流程。

隨著系統核心功能與端對端測試趨於完善，為確保多人協同開發與 AI 代理人提交時能夠**即時攔截缺陷、維持規格一致性、保障提交歷史清晰可溯**，亟需建立一套兼具「秒級回饋」與「深度驗收」的 **GitHub Actions 持續整合（CI）規範**。

### 1.1 核心痛點與挑戰
1. **測試耗時與反饋速度的矛盾**：
   - 專案包含 50+ 筆單元測試（Surefire）與 10+ 筆 Playwright E2E 真機瀏覽器測試（Failsafe）。若每次 PR 提交皆完整啟動無頭瀏覽器並下載系統依賴，CI 耗時將高達 3~6 分鐘，嚴重拖慢開發反饋週期。
2. **規格與代碼脫鉤風險**：
   - 採用 OpenSpec 驅動開發時，若 PR 變更了功能卻未同步修訂規格，或引入不合法的規格 YAML/Markdown，將造成規格文件腐化。
3. **Commit 訊息失控風險**：
   - 專案嚴格要求 Conventional Commits 格式且必須使用繁體中文簡述。若無機械化工具阻擋，易混入格式錯誤或純英文的 Commit，污染 Git Log 主幹歷史。

---

## 2. 現有代碼庫與測試環境盤點 (Codebase & Test Suite Audit)

經檢視專案之 [pom.xml](../../pom.xml)、[application-test.yml](../../src/test/resources/application-test.yml) 與 [openspec/config.yaml](../../openspec/config.yaml)，CI 管線設計依託於以下現況基礎：

```
+-----------------------------------------------------------------------------------------+
|                                現有代碼庫之 CI 關鍵特徵矩陣                              |
+----------------------+------------------------------------------------------------------+
| 維度                 | 現況細節與依據                                                   |
+----------------------+------------------------------------------------------------------+
| 執行環境與 JDK        | Ubuntu Linux Runner, Java 21 (Temurin / Corretto)                |
| 建置工具             | Apache Maven (專案內建 mvnw 與 mvnw.cmd Wrapper)                |
| 單元測試 (Surefire)  | maven-surefire-plugin 已排除 *IT.java 與 *E2ETest.java           |
|                      | 純記憶體執行，依賴 H2 In-Memory (MODE=MSSQLServer)，秒級完成     |
| 整合與 E2E (Failsafe)| maven-failsafe-plugin 包含 *IT.java 與 *E2ETest.java             |
|                      | 採用 Microsoft Playwright for Java (v1.46.0) 進行無頭瀏覽器驗證  |
| 規格驅動工具         | OpenSpec CLI (@fission-ai/openspec)，提供 openspec validate 指令 |
| 提交規範限制         | 格式：<type>(<scope>): <繁體中文簡述>，禁止英文主旨與句號結尾     |
+----------------------+------------------------------------------------------------------+
```

---

## 3. CI 雙層管線架構設計 (Two-Tier CI Architecture)

依據探索決策，我們確立 **「PR 快速門禁（Fail Fast）」** 與 **「Main 深度整合（Deep Integration）」** 的分流架構：

```
============================== GitHub Actions CI 架構圖 ==============================

   [開發者建立 / 更新 Pull Request]                       [合併 / Push 至 main 主分支]
                 |                                                      |
                 v                                                      v
  +-------------------------------+                      +-------------------------------+
  |  Job 1: pr-compliance        |                      |  Job 1: main-deep-test        |
  |  (規範合規檢驗，強制阻擋)      |                      |  (完整回歸測試與封裝打包)      |
  |  * PR Title / Commits 格式校驗|                      |                               |
  |    - Conventional Commits     |                      |  Step 1: Setup Java 21        |
  |    - 指定 Scope 清單驗證      |                      |  Step 2: Maven Cache 恢復     |
  |    - 強制繁體中文檢核         |                      |  Step 3: Playwright 快取恢復  |
  |  * OpenSpec 規格一致性檢驗    |                      |  Step 4: mvn clean test (單元)|
  |    - openspec validate --all  |                      |  Step 5: mvn verify (E2E/IT)  |
  +-------------------------------+                      |  Step 6: mvn package 打包     |
                 |                                       +-------------------------------+
                 v                                                      |
  +-------------------------------+                                     v
  |  Job 2: pr-unit-test          |                      +-------------------------------+
  |  (極速單元驗證，耗時 < 1 分鐘) |                      |  Job 2: publish-artifacts     |
  |  * Setup Java 21 + M2 Cache   |                      |  (產出物與測試報告收集)        |
  |  * mvn clean test             |                      |  * Surefire/Failsafe HTML 報告|
  |  * 純 H2 記憶體 DB，排除 E2E  |                      |  * Playwright 失敗截圖與錄影   |
  +-------------------------------+                      |  * Spring Boot 可執行 JAR 檔  |
                 |                                       +-------------------------------+
                 v
       [兩者皆綠燈才允許 Merge]
```

---

## 4. 核心守門員機制詳細規格 (Quality Gates Detailed Specs)

### 4.1 Commit 訊息與 PR 標題合規檢驗 (pr-compliance)

當 PR 發起時，管線將檢驗 **PR 標題（PR Title）** 以及 **分支內每一筆 Commit Message** 是否完全吻合專案規定。

* **正規表達式（Regex Pattern）**：
  ```regex
  ^(feat|fix|refactor|perf|test|style|docs|chore|revert)\((controller|service|repository|entity|dto|config|security|exception|view|common|build|specs)\): [\s\S]*[\u4e00-\u9fa5]+[\s\S]*$
  ```
* **校驗規則**：
  1. **Type 集合**：僅允許 `feat`, `fix`, `refactor`, `perf`, `test`, `style`, `docs`, `chore`, `revert`。
  2. **Scope 集合**：僅允許 `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`, `exception`, `view`, `common`, `build`, `specs`。
  3. **繁體中文限制**：主旨簡述內**必須包含中文字元**（`[\u4e00-\u9fa5]`），禁止純英文 Commit。
  4. **符號限制**：結尾不得帶有句號 `。` 或 `.`。
* **違規範例與正確範例**：
  - ❌ `feat(auth): add jwt token provider`（違規：未包含繁體中文）
  - ❌ `fix: 修復登入邏輯`（違規：缺少明確 Scope）
  - ❌ `update(service): 更新查詢效能`（違規：非合法 Type）
  - ✅ `feat(security): 實作密碼複雜度原則與配置屬性`
  - ✅ `test(service): 補充記帳類別服務之階層切換測試`

### 4.2 OpenSpec 規格合法性驗證 (openspec-validation)

為防止無效、語法損毀或不符合 Schema 的規格檔案被併入代碼庫，CI 環境中透過 Node.js 執行 OpenSpec 驗證：
```bash
npm install -g @fission-ai/openspec
openspec validate --all
```
若有規格未完成定義（如缺失必填欄位或 Task 未標註狀態），CI 將拋出非零 Exit Code 中斷流程。

### 4.3 快速單元測試守門 (pr-unit-test)

* **目標**：在 1 分鐘內驗證 Service、DTO 驗證、加解密與工具邏輯。
* **執行指令**：
  ```bash
  mvn clean test -Dspring.profiles.active=test
  ```
* **環境特性**：使用 `application-test.yml` 啟用 H2 In-Memory DB，不啟動實體資料庫，不啟動 Playwright 瀏覽器核心。

### 4.4 主幹深度整合與 Playwright 快取策略 (main-deep-test)

合併進入 `main` 分支後，觸發全套端對端檢驗：
* **執行指令**：
  ```bash
  mvn clean verify
  ```
* **Playwright 瀏覽器快取機制**：
  Playwright 在 Linux 容器環境下預設安裝瀏覽器於 `~/.cache/ms-playwright`。透過 GitHub Actions 的 `actions/cache` 機制，能夠將已安裝的 Chromium 核心進行持久化快取：
  ```yaml
  - name: Cache Playwright Browsers
    uses: actions/cache@v4
    with:
      path: ~/.cache/ms-playwright
      key: playwright-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}
      restore-keys: |
        playwright-${{ runner.os }}-
  ```
* **系統相依套件安裝**：
  若快取未命中，則透過 Playwright CLI 補齊瀏覽器二進位檔及其 Linux 依賴庫：
  ```bash
  mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"
  ```

### 4.5 測試失敗報告與產物保存 (Artifacts Collection)

當 E2E 測試或建置遭遇失敗時，CI 會自動打包並上傳以下目錄至 GitHub Artifacts（保留 7 天）：
1. `target/surefire-reports/`（單元測試詳細報表）
2. `target/failsafe-reports/`（E2E / 整合測試報表）
3. `target/playwright-traces/` 或截圖（若測試有啟用 Playwright 追蹤/截圖）
4. 成功時產出之可執行檔：`target/daily-ledger-system-0.0.1-SNAPSHOT.jar`

---

## 5. GitHub 倉庫治理與分支保護規則 (Repository Governance)

為了落實 CI 規範的強制力，GitHub Repository 需配置以下 **Branch Protection Rules**（針對 `main` 分支）：

```
+-------------------------------------------------------------------------------+
|                      GitHub Branch Protection 設定清單 (main)                  |
+-------------------------------------------------------------------------------+
| [v] Require a pull request before merging                                     |
|     * Require approvals: 1                                                    |
|     * Dismiss stale pull request approvals when new commits are pushed       |
|                                                                               |
| [v] Require status checks to pass before merging                              |
|     * Require branches to be up to date before merging                        |
|     * 必填通過檢查項目 (Required Checks):                                     |
|       - pr-compliance (Commit 格式與 OpenSpec 規格驗證)                         |
|       - pr-unit-test  (Java 21 單元測試通過)                                  |
|                                                                               |
| [v] Require linear history                                                    |
| [v] Do not allow bypassing the above settings (管理者亦需遵守)                  |
+-------------------------------------------------------------------------------+
```

### 合併策略（Merge Strategy）建議
* **推薦使用 Squash and Merge**：
  在 GitHub 介面中將 PR 的多個零碎 commit 壓扁成單一 commit 併入 main。
  因為在 `pr-compliance` 中已嚴格檢查了 PR 標題符合 `<type>(<scope>): <繁體中文簡述>`，Squash Merge 會直接繼承該標題，確保 `main` 分支歷史乾淨、一致且完全符合規範。

---

## 6. GitHub Actions 工作流程實施藍圖 (Implementation Blueprints)

以下為後續落地實作之 Workflow 結構藍圖：

### 6.1 PR 門禁工作流 (`.github/workflows/ci-pr.yml`)

```yaml
name: "PR Gatekeeper"

on:
  pull_request:
    branches: [ main ]
    types: [ opened, synchronize, reopened, edited ]

jobs:
  pr-compliance:
    name: "PR Compliance & OpenSpec Gate"
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Validate PR Title & Commits Convention
        run: |
          # 檢查 PR 標題與 Commit 訊息之 Conventional + 繁中規範
          TITLE="${{ github.event.pull_request.title }}"
          REGEX="^(feat|fix|refactor|perf|test|style|docs|chore|revert)\((controller|service|repository|entity|dto|config|security|exception|view|common|build|specs)\): .*"
          if [[ ! $TITLE =~ $REGEX ]]; then
            echo "::error::PR 標題不符合 Conventional Commits 規範: $TITLE"
            exit 1
          fi
          if [[ ! $TITLE =~ [\x{4e00}-\x{9fa5}] ]]; then
            echo "::error::PR 標題必須包含繁體中文簡述！"
            exit 1
          fi

      - name: Setup Node.js for OpenSpec
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install & Run OpenSpec Validate
        run: |
          npm install -g @fission-ai/openspec
          openspec validate --all

  pr-unit-test:
    name: "Fast Unit Tests"
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Run Unit Tests (H2 in-memory)
        run: ./mvnw clean test --no-transfer-progress
```

### 6.2 主分支深層驗證工作流 (`.github/workflows/ci-main.yml`)

```yaml
name: "Main Deep Verification"

on:
  push:
    branches: [ main ]

jobs:
  main-verify:
    name: "Full Verification & Packaging"
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Cache Playwright Browsers
        id: playwright-cache
        uses: actions/cache@v4
        with:
          path: ~/.cache/ms-playwright
          key: playwright-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            playwright-${{ runner.os }}-

      - name: Install Playwright Browsers & OS Dependencies
        run: |
          ./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"

      - name: Run Full Tests (Unit + IT + Playwright E2E)
        run: ./mvnw clean verify --no-transfer-progress

      - name: Build Executable BootJar
        run: ./mvnw package -DskipTests --no-transfer-progress

      - name: Upload Test Reports & Artifacts
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: |
            target/surefire-reports/
            target/failsafe-reports/
          retention-days: 7
```

---

## 7. 總結與落地方案路線圖 (Summary & Roadmap)

本次探索確立了專案持續整合的四大原則：
1. **快慢分離**：PR 著重極速單元驗證（< 1 分鐘），Main 著重全量 Playwright E2E 與打包驗收。
2. **規格先行**：將 OpenSpec 驗證內建於 CI 門禁中，規格損壞即拒絕合併。
3. **歷史整潔**：透過機械化正規表達式強制落實 Conventional Commits 與繁體中文標題規範。
4. **資源節省**：透過 Maven 依賴與 Playwright 瀏覽器雙快取機制，大幅降低 GitHub Actions 算力與等待時間。

### 後續落地路線圖
* **Phase 1**：發起 OpenSpec Change Proposal（`setup-github-actions-ci`）。
* **Phase 2**：建立 `.github/workflows/ci-pr.yml` 與 `.github/workflows/ci-main.yml`。
* **Phase 3**：在 GitHub 倉庫後台啟用 Branch Protection Rules，完成 CI 守門員正式上線。
