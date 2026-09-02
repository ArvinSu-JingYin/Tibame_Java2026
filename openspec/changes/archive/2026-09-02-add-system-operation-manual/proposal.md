## Why

目前日常流水帳系統 (`daily-ledger-system`) 已具備完整的技術架構、資料庫 DDL、Spring Boot 後端與純離線 Vue 3 瑞士風格前端等規格文件 (`01~07`)，但缺乏一份專門針對終端使用者 (End-User)、操作員與評審檢驗的「系統操作手冊 (User Operation Manual)」。為了使使用者能依循標準化操作步驟（SOP）與生活化實務案例快速上手登入、極速記帳、收支儀表板檢視、自訂分類管理與多維度查帳，特立本提案新增專屬之系統操作手冊文件。

## What Changes

- **新增系統操作手冊文件**：於 `docs/daily_ledger_system/08_system_operation_and_user_manual.md` 建立包含 8 大核心章節與 6 個完整情境案例 (Case-Based Walkthrough) 的詳細操作指南。
- **更新文件導覽目錄**：於 `docs/daily_ledger_system/README.md` 中將 `08_system_operation_and_user_manual.md` 納入系統文件清單索引。
- **章節涵蓋內容**：
  1. 系統存取與環境準備（案例 0：系統就緒與首頁訪問）
  2. 帳號註冊與會員登入（案例 1：新人小明初次啟用與 JWT 憑證）
  3. 主工作台與極速流水帳記帳操作（案例 2：單筆記帳與收支快速切換）
  4. 收支儀表板與即時統計連動（案例 3：檢視月度收支與淨結餘）
  5. 多維度組合搜尋與帳目過濾（案例 4：關鍵字/日期/收支/分類查帳）
  6. 帳目修改、刪除防呆與資料維護（案例 5：更正誤植金額與 SweetAlert2 確認）
  7. 分類管理與雙層防呆機制（案例 6：自訂毛小孩分類與內建防呆）
  8. 常見問題 (FAQ) 與疑難排解自救指南

## Capabilities

### New Capabilities
*(無，本變更為純文件規格變更，已設定 `skip_specs: true`)*

### Modified Capabilities
*(無，不變更底層 API 與系統行為規格契約)*

## Impact

- **受影響檔案**：
  - `docs/daily_ledger_system/08_system_operation_and_user_manual.md` (新增)
  - `docs/daily_ledger_system/README.md` (更新索引)
- **API 與系統影響**：無影響，不涉及程式碼與 API 規格異動。
