## Why

現有個人財務流水帳工作台採用單頁縱向堆疊佈局（Monolithic Layout），同時在單一頁面渲染月度指標、歷史流水與輸入欄位，且分類管理採用彈窗呈現。這導致使用者在結帳當下進行記帳時產生情境失焦與認知負荷過重，分類維護操作空間亦受限。為貫徹「極速捕捉第一 (Quick-Capture First)」核心理念，需要將工作台重構為情境化四大獨立分頁體系，登入後首頁聚焦於純粹金額輸入，並在入帳成功後自動流轉至明細分頁進行即時查核。

## What Changes

- **四大情境獨立分頁體系 (Swiss Tabs)**：將工作台劃分為「01 記帳錄入 (Quick Entry)」、「02 交易明細 (Ledger History)」、「03 財務概覽 (Financial Overview)」與「04 分類管理 (Category Management)」，由 Vue 3 反應式狀態驅動純前端無刷新切換。
- **登入後聚焦首頁 (Focus Entry First)**：登入完成後預設進入「01 記帳錄入」，畫面僅保留純粹金額、收支類別、備註輸入與結構化/NLP 次模式切換，大幅降低視覺干擾。
- **入帳後自動流轉確認 (Auto Transition)**：在記帳錄入分頁成功送出交易後，系統提示幾何 Swiss Toast，並平滑自動切換至「02 交易明細」分頁，立即展示最新流水記錄供使用者確認。
- **獨立分類管理平鋪頁面**：取代原本狹窄的 Bootstrap Modal 彈窗，將雙層分類維護升格為獨立分頁「04 分類管理」，平鋪展示系統保護分類與個人自訂分類，提供清晰直覺的新增與刪除操作。
- **財務概覽獨立分析**：將年份月份選擇控制器與總收入、總支出、淨結餘幾何卡片收攏於「03 財務概覽」分頁，專注提供宏觀財務盤點。

## Capabilities

### New Capabilities
<!-- 無新增 Capability -->

### Modified Capabilities
- `offline-web-ui`: 更新記帳工作台介面規格，引入四大獨立分頁導覽架構、登入後純粹錄入聚焦、記帳成功後自動流轉明細、以及全寬獨立分類管理分頁。

## Impact

- **前端視圖與腳本 (Affected Files)**：
  - `src/main/resources/templates/ledger.html`：重構 HTML 佈局結構，導入 Swiss Tab 導覽列與四個分頁容器區塊，移除 Modal 彈窗。
  - `src/main/resources/static/css/swiss-style.css`：新增 Swiss Tab 切換樣式、高對比標籤高亮與純粹記帳視圖樣式。
  - `src/main/resources/static/js/pages/ledger.js`：導入 `activeTab` 反應式狀態，整合記帳成功後的自動分頁流轉與數據重載邏輯。
- **後端 Web API 與資料庫**：既有 REST API 契約 (`/api/v1/records`, `/api/v1/categories`, `/api/v1/records/summary`) 保持 100% 相容，無任何後端破壞性變更。
- **自動化測試 (E2E Testing)**：依據探索結論，本次變更專注於前端介面體驗，E2E 測試線束暫不連動修改。
