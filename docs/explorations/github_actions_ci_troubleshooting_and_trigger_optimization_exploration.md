# GitHub Actions CI 觸發失效排查、雙分支支援與手動觸發相容性探索報告 (GitHub Actions CI Troubleshooting & Trigger Optimization Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：GitHub Actions / Git Branching / Conventional Commits / OpenSpec CLI / Node.js 20 / Java 21  
> **目標範疇**：排查 CI Workflow 無法觸發之根本原因、雙分支 (`dev` + `main`) 觸發矩陣重構、手動觸發 (`workflow_dispatch`) 相容性評估與 PR 標題檢驗相容對策  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題描述 (Background & Problem Description)

在專案完成 GitHub Actions 雙層持續整合（CI）管線（PR 快速門禁 `ci-pr.yml` 與主幹深度驗收 `ci-main.yml`）建置後，開發團隊將代碼自本地端（Local）推送至 GitHub 倉儲，卻發現 **GitHub Actions 頁籤完全沒有任何 Workflow Runs 啟動或運行**。

為查明管線未觸發之根本成因，並確立穩健、彈性的觸發與手動執行機制，啟動本專題探索。

---

## 2. 根本原因深入排查 (Root Cause Investigation)

透過對本地與遠端 Git 拓撲、工作流程定義檔進行交叉稽核，定位出 **3 大根本原因**：

### 2.1 原因一：推送分支與 Workflow 監聽分支不匹配 (Branch Mismatch)
* **遠端分支現狀**：
  - 專案本地目前處於 `dev` 分支。
  - GitHub 遠端預設分支為 `origin/dev`（`remotes/origin/HEAD -> origin/dev`），原本遠端**尚未建立 `main` 分支**。
* **Workflow 觸發定義**：
  - `ci-main.yml` 原定義：
    ```yaml
    on:
      push:
        branches:
          - main # <--- 僅監聽 main 分支的 push
    ```
  - 當開發者執行 `git push origin dev` 時，GitHub 接收到分支為 `dev` 的 Push 事件，比對後發現不符合 `main`，直接略過，不產生任何 Run。

### 2.2 原因二：PR 快速門禁事件類型限制 (Event Type Restriction)
* `ci-pr.yml` 原定義：
  ```yaml
  on:
    pull_request:
      branches:
        - main
  ```
* 該工作流程僅響應 `pull_request` 事件（開啟、更新、重新開啟、編輯 PR）。單純的 `git push`（無論是推送到 `dev` 或 `main`）皆不會觸發 PR 事件。

### 2.3 原因三：缺少手動觸發機制 (`workflow_dispatch`)
* 兩個核心 Workflow 皆未配置 `workflow_dispatch:`，導致開發者無法在 GitHub 網頁 Actions 介面點擊「Run workflow」按鈕進行手動排查或即時驗收。

---

## 3. 事件派發流向比對 (Event Flow Architecture)

```
+-------------------------------------------------------------------------------------------------+
|                                  事件觸發流向與條件比對圖                                       |
+-------------------------------------------------------------------------------------------------+
|                                                                                                 |
|   [本地 Local]                                                                                  |
|        |                                                                                        |
|        |  git push origin dev                                                                   |
|        v                                                                                        |
|   [GitHub 遠端 (origin)]                                                                        |
|        |                                                                                        |
|        +--> 收到 Push 事件 (Branch: "dev")                                                      |
|                 |                                                                               |
|                 |-- 比對 ci-main.yml:                                                           |
|                 |   要求: push to [main]  --------> [X] 分支不符 (dev != main)                  |
|                 |                                                                               |
|                 |-- 比對 ci-pr.yml:                                                             |
|                 |   要求: pull_request to [main] -> [X] 事件不符 (非 PR 事件)                   |
|                 |                                                                               |
|                 |-- 比對 copilot-setup-steps.yml:                                               |
|                 |   要求: 限定路徑變更    --------> [X] 未異動該檔案                            |
|                 |                                                                               |
|                 v                                                                               |
|   [結果]: 所有工作流程皆未命中觸發規則，Actions 頁面顯示為空 (0 Runs)!                           |
|                                                                                                 |
+-------------------------------------------------------------------------------------------------+
```

---

## 4. 關鍵技術相容性發現：手動觸發相容性對策

在評估為 `ci-pr.yml` 新增 `workflow_dispatch` 時，發現了一項**隱含的執行期缺陷**：

### 4.1 潛在缺陷分析
在 `ci-pr.yml` 的 `pr-compliance` 任務中，包含 Node.js 腳本校驗 PR 標題：
```javascript
const prTitle = process.env.PR_TITLE || "";
...
function validate(name, text) {
  const trimmed = (text || "").trim();
  if (!trimmed) {
    console.error(`::error::[FAIL] ${name} 不能為空`);
    hasError = true;
    return;
  }
  ...
}
validate("PR Title", prTitle);
```
* 當透過 `pull_request` 事件觸發時，`github.event.pull_request.title` 存在，驗證正常。
* 但當透過 `workflow_dispatch` 手動觸發時，**不存在 PR 上下文**，`PR_TITLE` 為空字串。
* 若不加以防護，手動觸發將會直接觸發 `[FAIL] PR Title 不能為空`，導致手動驗收直接判定失敗（Exit Code 1）。

### 4.2 相容性防禦解法
在注入環境變數時傳入 `EVENT_NAME: ${{ github.event_name }}`，並於腳本中進行條件分支：
```javascript
const eventName = process.env.EVENT_NAME || "";
if (eventName === "pull_request") {
  console.log("=== 正在校驗 PR 標題 ===");
  validate("PR Title", prTitle);
} else {
  console.log("非 PR 事件（如手動觸發 workflow_dispatch），略過 PR 標題校驗。");
}
```
如此一來，手動觸發時僅會執行 **OpenSpec 規格檢驗** 與 **單元測試守門**，達成無痛手動驗收。

---

## 5. 方案評估與決策收斂 (Options & Decision)

針對分支治理架構，評估以下兩種策略：

```
+-------------------------------------------------------------------------------------------------+
| 方案比較矩陣                                                                                    |
+----------------------+------------------------------------+-------------------------------------+
| 評估維度             | 方案 A：雙分支支援 + 手動觸發 (採納)| 方案 B：純 main 分支模式            |
+----------------------+------------------------------------+-------------------------------------+
| 適用分支             | main 與 dev 雙軌監聽               | 僅 main                             |
| Push 觸發            | push 到 main 或 dev 均觸發全量驗收 | 僅 push 到 main 觸發                |
| PR 觸發              | 目標為 main 或 dev 之 PR 均會守門  | 僅目標為 main 之 PR 守門            |
| 手動觸發             | 支援 workflow_dispatch             | 不支援或僅 main 支援                |
| 協作流暢度           | 極高，本地 push dev 立即享有 CI 守門| 需頻繁發 PR 或切換 main 分支        |
| 適用場景             | 中小型敏捷團隊、日常在 dev 迭代     | 嚴格 Git Flow、所有變更必走 PR 審查 |
+----------------------+------------------------------------+-------------------------------------+
```

### 決策結論
**全面採納「方案 A」**：
1. 本地與遠端均保留 `dev` 作為日常開發主軌，同時保留 `main` 作為發布主幹。
2. `ci-main.yml` 調整為監聽 `main` 與 `dev` 的 push，並加入 `workflow_dispatch`。
3. `ci-pr.yml` 調整為監聽目標為 `main` 與 `dev` 的 pull_request，並加入相容版 `workflow_dispatch`。

---

## 6. 目標觸發矩陣 (Target Trigger Matrix)

```
+-------------------------------------------------------------------------------------------------+
| 工作流程 (Workflow)        | 自動觸發事件 (Events)     | 監聽分支 (Branches) | 手動觸發 (Dispatch)       |
+----------------------------+---------------------------+---------------------+---------------------------+
| ci-main.yml (深度全量驗收) | push                      | [main, dev]         | 支援 (可選分支手動執行)   |
| ci-pr.yml (PR 快速守門)    | pull_request              | [main, dev]         | 支援 (自動略過無 PR 標題) |
| copilot-setup-steps.yml    | push, pull_request        | (限定檔案異動)      | 已支援                    |
+-------------------------------------------------------------------------------------------------+
```

---

## 7. 後續落地藍圖 (Implementation Roadmap)

本探索已收斂至可執行階段，後續將依據 OpenSpec 規格驅動流程建立變更提案：

1. **OpenSpec 變更提案**：
   - 變更名稱：`support-dual-branch-and-manual-ci-triggers`
   - 規格同步：修訂 `openspec/specs/ci-pipeline/spec.md` 規範條款
2. **工作流程代碼實施**：
   - 更新 `.github/workflows/ci-main.yml`
   - 更新 `.github/workflows/ci-pr.yml`
3. **治理文件同步**：
   - 修訂 `docs/guides/github-actions-ci-guide.md` 之觸發章節與架構圖
   - 更新 `docs/README.md` 全域索引門戶
