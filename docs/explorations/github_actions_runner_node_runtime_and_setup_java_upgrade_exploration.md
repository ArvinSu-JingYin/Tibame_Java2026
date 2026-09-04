# GitHub Actions Runner Node.js 執行期淘汰、actions/setup-java@v5 升級與 Node 22 LTS 遷移探索報告 (GitHub Actions Runner Node Runtime & Actions Upgrade Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：GitHub Actions / Node.js 22 LTS / Node.js 24 / actions/setup-java@v5 / OpenSpec CLI / Temurin JDK 21  
> **目標範疇**：GitHub Actions Runner 淘汰 Node.js 20 警示成因排查、`actions/setup-java@v4` 升級相容性評估、PR CI 門禁 Node.js 22 LTS 選型決策、規格書與工作流程連動影響評估  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題發現 (Background & Problem Discovery)

在專案主幹與分支保護持續整合（CI）管線運作過程中，GitHub Actions 的 Workflow 執行日誌（Log）中拋出了以下兩則顯著的黃色即將棄用警告（Deprecation Warning）：

```text
[Main Integration & Verification] Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/cache@v4, actions/checkout@v4, actions/setup-java@v4, actions/upload-artifact@v4. For more information see: https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/

[Main Integration & Verification] setup-java v4 is deprecated and will no longer receive updates. Please migrate to actions/setup-java@v5.
```

雖然當前的建置、單元測試、E2E 瀏覽器驗收與產物打包依然全數通過（狀態為綠燈），但為防範未然、杜絕技術債積累，並確保專案 CI 管線符合 GitHub 官方標準架構，啟動本專題探索。

---

## 2. 警告成因深度解構 (Warning Mechanism Deconstruction)

### 2.1 警告一：Runner 底層 Node.js 20 執行期淘汰 (Forced Node 24 Runtime)
* **官方政策變更**：
  - 根據 GitHub 官方公告（[Deprecation of Node 20 on GitHub Actions runners](https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/)），雲端託管 Runner（Hosted Runners，如 `ubuntu-latest`）已逐步終止對 Node.js 20 執行環境的正式支援，將底層預設環境升級為 **Node.js 24**。
* **觸發機制**：
  - 開發團隊使用的標準 Action（例如 `actions/checkout@v4`、`actions/cache@v4`、`actions/upload-artifact@v4`、`actions/setup-java@v4`）在釋出時，其內部定義檔案 `action.yml` 宣告 `runs.using: 'node20'`。
  - Runner 在遇到宣告使用 `node20` 的 Action 時，為了避免使用者的 Pipeline 直接崩潰中斷，採取了向後相容機制：**「強制使用 Node.js 24 來執行該 Action (forced to run on Node.js 24)」**，並於 Log 中輸出 Warning 提醒維護者遷移。

### 2.2 警告二：`actions/setup-java@v4` 官方生命週期終止 (End-of-Life)
* **淘汰現狀**：
  - GitHub 官方已將 `actions/setup-java@v4` 列入正式 Deprecated 狀態，不再接收新功能、安全性修補或對新版 Node 執行期的升級支援。
* **官方推薦遷移路徑**：
  - 官方已正式發布 **`actions/setup-java@v5`**，全面轉向原生 Node.js 24 執行期架構。

### 2.3 現行專案工作流程引用盤點矩陣
經全面稽核代碼庫中之 GitHub Actions 定義檔，受影響位置如下：

| 工作流程檔案 | 涉及任務 (Job) | 當前宣告版本 | 警告類型 |
| :--- | :--- | :--- | :--- |
| **`.github/workflows/ci-main.yml`** | `main-verify` | `actions/setup-java@v4` (L19) | 官方 EOL 警告 + Node 24 強轉 |
| **`.github/workflows/ci-pr.yml`** | `pr-compliance` | `node-version: 20` (L31) | 舊版 Node 執行環境 |
| **`.github/workflows/ci-pr.yml`** | `pr-unit-test` | `actions/setup-java@v4` (L123) | 官方 EOL 警告 + Node 24 強轉 |
| **`.github/workflows/copilot-setup-steps.yml`** | `copilot-setup-steps` | 無 setup-java | 僅 checkout@v4 (Runner 相容性執行中) |

---

## 3. 技術選型與決策矩陣 (Technical Trade-offs & Decision Matrix)

### 3.1 `actions/setup-java` 升級為 `@v5` 相容性評估
針對專案現有用法進行參數對比：

```yaml
# 現有配置
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'maven'
```

* **參數相容性**：
  - `java-version: '21'`：完全相容。
  - `distribution: 'temurin'`：完全相容（Eclipse Temurin 仍為官方推薦首選發行版）。
  - `cache: 'maven'`：完全相容，內部快取解析與 Maven Wrapper 支援機制維持一致。
* **結論**：升級為 `actions/setup-java@v5` 為**無損升級（Drop-in Replacement）**，零 Breaking Change，可立即消除 EOL 警示。

---

### 3.2 PR CI 門禁 Node.js 運行版本選型：Node 20 vs Node 22 LTS vs Node 24
在 `ci-pr.yml` 的 `pr-compliance` 任務中，Node.js 主要承擔兩大職責：
1. 全域安裝並執行 `@fission-ai/openspec` 進行規格檔（`openspec validate --all`）語法與架構校驗。
2. 執行內聯腳本 `node -e '...'`，透過 `child_process.execSync` 與常規正則運算式校驗 PR 標題與 Commit 訊息之繁中規範。

各版本比較矩陣如下：

| 評估維度 | Node.js 20 (現況) | Node.js 22 (決策首選) | Node.js 24 |
| :--- | :--- | :--- | :--- |
| **官方維護狀態** | Maintenance LTS（即將終止支援） | **Active LTS (代號 Jod)** | Current（即將轉為 LTS） |
| **企業級穩定度** | 穩定但逐步淘汰 | **極高（目前主流企業 CI 標準）** | 高（最新技術試驗） |
| **npm 生態相容性** | 廣泛 | **最廣泛、最穩健**（OpenSpec CLI 最佳相容） | 大多相容，少數 native bindings 待觀察 |
| **CI Runner 協同性** | 引發淘汰警告 | **原生支援，無任何警告** | 與 Runner Node 24 底層完全對齊 |
| **選型建議** | ❌ 儘早淘汰 | ✅ **最佳平衡首選（Active LTS）** | ⚠️ 可行，但 LTS 優先 |

* **決策結論**：經評估，**選定升級至 Node.js 22 (Active LTS)**。既能消除 Runner 警示，又能提供最高度穩健的 OpenSpec 工具鏈相容性。

---

## 4. 雙層管線架構拓撲圖 (Pipeline Architecture Topology)

```
+-------------------------------------------------------------------------------+
|                        GitHub Actions Runner (Ubuntu Latest)                  |
+-------------------------------------------------------------------------------+
                                         |
               +-------------------------+-------------------------+
               |                                                   |
               v                                                   v
    +-----------------------+                           +----------------------+
    | ci-pr.yml (PR Gate)   |                           | ci-main.yml (Main)   |
    +-----------------------+                           +----------------------+
    |                       |                           |                      |
    | * actions/checkout@v4 |                           | * actions/checkout@v4|
    |                       |                           |                      |
    | * actions/setup-node  |                           | * actions/setup-java |
    |   node-version: 22    |                           |   @v5 (Node 24 原生) |
    |   [升級為 Active LTS] |                           |   [取代已棄用之 v4]  |
    |                       |                           |                      |
    | * npm openspec        |                           | * actions/cache@v4   |
    |   validate --all      |                           |                      |
    |                       |                           | * ./mvnw verify      |
    | * Conventional Commit |                           |   (單元 + 整合 + E2E)|
    |   Linter (Node 22)    |                           |                      |
    |                       |                           | * actions/upload-    |
    | * actions/setup-java  |                           |   artifact@v4        |
    |   @v5 (Node 24 原生)  |                           |   (報告與 JAR 歸檔)  |
    |   [取代已棄用之 v4]   |                           |                      |
    |                       |                           +----------------------+
    | * ./mvnw clean test   |
    +-----------------------+
```

---

## 5. 連動影響範圍與 OpenSpec 規格治理 (Governance & Scope Impact)

升級不僅限於修改工作流程檔案，應落實全生命週期治理：

### 5.1 OpenSpec 規格書同步 (`openspec/specs/ci-pipeline/spec.md`)
現行規格書之 Requirement 20 行定義：
```markdown
#### Scenario: OpenSpec specification validity verification
- **WHEN** a pull request is submitted or updated targeting `main` or `dev`, or triggered manually
- **THEN** the `pr-compliance` job MUST install OpenSpec tooling (`@fission-ai/openspec`) under Node.js 20 and execute `openspec validate --all`, failing the check if any syntax, schema, or incomplete artifact validation errors are detected
```
* **調整方案**：應透過 OpenSpec 變更提案，將此處之 `Node.js 20` 修訂為 `Node.js 22`，保持「規格即真實（Specs as SSOT）」之一致性。

### 5.2 操作指南與架構手冊維護 (`docs/guides/github-actions-ci-guide.md`)
* 檢視並更新手冊中關於 PR 門禁環境之版本說明與依賴配置，確保開發者參考指南時獲得最新資訊。

---

## 6. 落地推進路線圖與後續步驟 (Implementation Roadmap)

依據標準 OpenSpec 變更管理流程，後續推進步驟拆解如下：

```
+------------------------------------------------------------------------------------+
|                       後續實施推進路線圖 (Execution Roadmap)                       |
+------------------------------------------------------------------------------------+
|                                                                                    |
|   1. 提案階段 (/opsx-propose)                                                      |
|      - 建立變更名稱: upgrade-ci-actions-and-node-runtime                            |
|      - 產出變更產物: proposal.md / design.md / tasks.md / specs delta              |
|                                                                                    |
|   2. 實施階段 (/opsx-apply)                                                        |
|      - 修改 .github/workflows/ci-main.yml (setup-java@v5)                          |
|      - 修改 .github/workflows/ci-pr.yml (setup-java@v5 + node-version: 22)         |
|      - 更新 docs/guides/github-actions-ci-guide.md                                 |
|                                                                                    |
|   3. 驗證階段 (Local Verification Gate)                                            |
|      - 執行 openspec validate --all 確保規格無破壞                                 |
|      - 執行 ./mvnw clean test 本地預檢                                             |
|                                                                                    |
|   4. 歸檔與同步 (/opsx-archive & /opsx-sync)                                       |
|      - 變更歸檔，將 Delta 規格同步合併至 openspec/specs/ci-pipeline/spec.md        |
|                                                                                    |
+------------------------------------------------------------------------------------+
```
