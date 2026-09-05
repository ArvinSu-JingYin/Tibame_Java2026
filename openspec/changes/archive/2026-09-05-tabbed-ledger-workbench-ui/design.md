## Context

現有工作台在 `src/main/resources/templates/ledger.html` 與 `src/main/resources/static/js/pages/ledger.js` 採用單頁縱向堆疊佈局。所有功能（月份導覽、統計指標、記帳表單、歷史流水表格）均於單一畫面垂直排版，分類管理則透過 Bootstrap Modal 彈窗呈現。
前端運行於嚴格離線 (No-CDN) 之 Vue 3 與 Bootstrap 5.3 環境，樣式由 `swiss-style.css` 定義高對比、幾何網格與瑞士風格規範。

相關動機與業務場景推演詳見 [proposal.md](proposal.md) 與探索報告 [docs/explorations/tabbed_ledger_workbench_and_focus_entry_exploration.md](../../docs/explorations/tabbed_ledger_workbench_and_focus_entry_exploration.md)。

## Goals / Non-Goals

**Goals:**
- **四大分頁架構**：在 `ledger.html` 與 `ledger.js` 構建 `activeTab` 反應式分頁體系，包含：
  - `01 記帳錄入 (entry)`：純粹金額、收支類別、備註輸入與結構化/NLP 雙模式，游標自動聚焦。
  - `02 交易明細 (history)`：多條件組合篩選器、歷史流水帳分頁表格、記錄編輯與刪除。
  - `03 財務概覽 (analytics)`：年月選擇控制器與總收入、總支出、淨結餘幾何卡片。
  - `04 分類管理 (categories)`：全寬平鋪呈現收支分類清單，支援新增與刪除自訂分類。
- **入帳後自動流轉**：記帳送出成功後，觸發幾何 Swiss Toast，自動切換至 `history` 分頁並刷新流水與統計數據。
- **瑞士風格標籤樣式**：在 `swiss-style.css` 建立幾何黑白純對比、大寫字母索引與瑞士紅強調線條之分頁標籤樣式。

**Non-Goals:**
- **不變更後端 API**：完全沿用 `/api/v1/records`、`/api/v1/categories`、`/api/v1/records/summary` 等 REST API 契約。
- **不引入外部路由套件**：不依賴 Vue Router，嚴格遵循專案離線原則，採用單頁反應式條件渲染 (`v-show` / `v-if`)。
- **暫不改動 E2E 測試線束**：依據探索決策，Playwright 驗收腳本 (`LedgerPage.java`) 解耦於下一階段擴充 `clickTab()` 支援。

## Decisions

### 決策 1：採用 Vue 3 反應式狀態 `activeTab` 實現純前端分頁切換
- **方案**：以 `const activeTab = ref('entry');` 管理當前分頁，透過分頁導覽列綁定點擊事件動態切換視圖容器。
- **權衡理由**：無須引入額外離線依賴或 Vue Router，換頁反應時間為 0 毫秒，且易於在組件與函式之間流轉。
- **替代方案評估**：
  - *多頁 Thymeleaf 模板*：換頁需重新發出 HTTP 請求並重新載入資產，破壞 SPA 流暢體驗。
  - *Vue Router*：專案為離線輕量架構，引入 Vue Router 會增加依賴包管理負擔與初始化複雜度。

### 決策 2：記帳成功後自動流轉至「02 交易明細」
- **方案**：`handleCreateRecord` 成功完成 API 呼叫後，執行 `activeTab.value = 'history'`，並並行呼叫 `loadRecords()` 與 `loadSummary()`。
- **權衡理由**：完成「記帳 -> 即時核對」的閉環操作體驗，確保使用者立刻在歷史清單首行看到剛剛寫入的記錄，降低心理疑慮。
- **替代方案評估**：
  - *停留在記帳錄入頁面*：使用者無法直觀確認資料是否已真實入庫，需要額外手動切換至歷史明細分頁。

### 決策 3：分類管理由 Modal 彈窗升格為獨立全寬分頁
- **方案**：移除原 `categoryModal` 彈窗，將其結構重構為 `activeTab === 'categories'` 時渲染之獨立全寬卡片。
- **權衡理由**：提供足夠的水平空間展示分類名稱、圖示、類型與系統保護狀態，避免彈窗捲動條擁擠問題。
- **替代方案評估**：
  - *維持 Modal 彈窗*：在行動裝置或較低解析度螢幕上檢視雙層分類時操作空間受限，且與頂部導覽列按鈕形成不一致的交互層級。

## Risks / Trade-offs

- **[Risk] 連續快速記帳體驗中斷**：使用者若想連續記錄多筆不同交易，自動切換至明細頁面需要再點一次「01 記帳錄入」。
  - *Mitigation*：在記帳成功流轉後，保持分頁切換按鈕永遠顯著置頂；未來可於探索第二期加入「連續記帳模式」開關（打勾時不跳頁）。
- **[Risk] E2E 測試線束可能需要適配**：若既有自動化測試腳本假設所有 DOM 節點都在可見狀態，分頁切換隱藏的元素可能引發選取超時。
  - *Mitigation*：採用 `v-show` 保留 DOM 節點，或依探索規劃在下一期為 `LedgerPage.java` 實作分頁點擊切換輔助方法。

## Migration Plan

1. **樣式擴充**：於 `src/main/resources/static/css/swiss-style.css` 加入 `.swiss-tab-bar`、`.swiss-tab-item`、`.swiss-tab-item.active` 等樣式。
2. **範本調整**：重構 `src/main/resources/templates/ledger.html`，置入全域分頁列，將原有垂直區塊分拆包覆於對應的 `tab` 容器內。
3. **邏輯連動**：在 `src/main/resources/static/js/pages/ledger.js` 宣告 `activeTab`，綁定切換函式，更新 `handleCreateRecord` 轉跳行為。
4. **驗證**：手動瀏覽各分頁操作流暢度、驗證結構化與 NLP 記帳後的自動轉跳與數據同步。
