## 1. 樣式層擴充 (CSS Styling)

- [x] 1.1 在 `src/main/resources/static/css/swiss-style.css` 擴充 Swiss Tab 導覽列與分頁項目樣式（包含幾何邊框、高對比 active 狀態、次模式按鈕群組），並確認樣式語法無誤
- [x] 1.2 在 `src/main/resources/static/css/swiss-style.css` 擴充純粹記帳聚焦輸入框與分類管理表格排版樣式，並確認響應式排版正常

## 2. 視圖層重構 (HTML Templates)

- [x] 2.1 在 `src/main/resources/templates/ledger.html` 導入 Swiss Tab 導覽切換列（四大分頁標籤：01 記帳錄入、02 交易明細、03 財務概覽、04 分類管理），並以 `v-show` 包覆相應分頁視圖容器
- [x] 2.2 將快速記帳（結構化與 NLP 模式）隔離為獨立「01 記帳錄入」分頁容器，並配置金額輸入框自動聚焦屬性
- [x] 2.3 將月度選擇器與收支統計卡片抽離至獨立「03 財務概覽」分頁容器
- [x] 2.4 將原 Modal 彈窗之分類管理重構為獨立全寬「04 分類管理」分頁容器，並移除冗餘之彈窗標記

## 3. 前端腳本與狀態流轉 (Vue 3 Logic)

- [x] 3.1 在 `src/main/resources/static/js/pages/ledger.js` 引入 `activeTab` 反應式狀態（預設值為 `'entry'`），並提供 `switchTab(tabName)` 切換函式
- [x] 3.2 修改 `handleCreateRecord` 記帳提交邏輯：送出成功後觸發 Swiss Toast，重設輸入表單，自動流轉 `activeTab = 'history'` 並重新加載 `loadRecords()` 與 `loadSummary()`
- [x] 3.3 調整分類管理相關函式，確保在獨立分頁中新增與刪除分類後能即時更新分類清單與記帳下拉選單

## 4. 系統驗證與回歸測試 (Verification)

- [x] 4.1 啟動應用程式並手動驗證四大分頁（01 記帳錄入、02 交易明細、03 財務概覽、04 分類管理）切換與樣式渲染正常
- [x] 4.2 驗證 01 記帳錄入分頁之金額自動聚焦、結構化記帳與 NLP 記帳功能，確認入帳後自動流轉至 02 交易明細且清單即時更新
- [x] 4.3 執行後端 Maven 測試確認既有 REST API 與核心業務邏輯無回歸異常
