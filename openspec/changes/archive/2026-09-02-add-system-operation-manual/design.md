## Context

參見 [proposal.md](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/openspec/changes/add-system-operation-manual/proposal.md)。本專案已具備後端 Spring Boot 3.x 與前端純離線 Vue 3 瑞士風格記帳系統，技術文件 01~07 著重架構與 DDL，本手冊需以終端使用者 (End-User) 操作與實務場景為主軸，提供直觀、具體、包含案例與 ASCII 示意圖的 step-by-step 操作指南。

## Goals / Non-Goals

**Goals:**
- 提供一份清晰、結構化且具備實務案例（Case-Based Walkthrough）的系統操作手冊 (`08_system_operation_and_user_manual.md`)。
- 涵蓋所有核心功能操作（登入註冊、記帳、收支統計、多維搜尋、修改刪除、自訂分類與雙層防呆）。
- 補充常見問題 (FAQ) 與常見錯誤代碼自救排錯指南。
- 確保所有路徑與文件導覽在 `docs/daily_ledger_system/README.md` 中完整索引。

**Non-Goals:**
- 本手冊不包含底層 Java 程式碼重構或後端 API 改造。
- 本手冊不包含資料庫結構的物理調整（聚焦在使用者操作層面）。

## Decisions

### 決策 1：採用方案 A 檔案結構與命名規範
- **選擇**：放置於 `docs/daily_ledger_system/08_system_operation_and_user_manual.md`。
- **理由**：承接現有 `01~07` 文件的編號命名規則，保持模組目錄整體性與一致性。
- **替代方案評估**：放置於 `docs/` 根目錄會破壞現有子系統資料夾的內聚性。

### 決策 2：採用情境式實務案例 (Case-Based Walkthrough) 貫穿各章節
- **選擇**：設計「新人小明」的使用旅程（從註冊登入、紀錄日常星巴克與薪資、查看收支儀表板、多維搜尋特定餐飲開銷、建立毛小孩自訂分類到觸發刪除防呆）。
- **理由**：相較於純規格條文說明，生活化情境案例能讓使用者與評審立刻掌握系統操作精髓與防呆邏輯。

### 決策 3：採用純 ASCII 流程與介面示意圖
- **選擇**：使用符合 Swiss Style 的純 ASCII 直角框線（`+`, `-`, `|`）呈現工作台介面配置與操作時序。
- **理由**：符合純離線無外網環境下的跨終端渲染相容性。

## Risks / Trade-offs

- **[風險]** 介面欄位名稱若與實際 Vue 模板不一致可能造成困惑 → **[緩解措施]** 嚴格對照 `ledger.html` 與 `login.html` 之實際 DOM 標籤與提示文字進行撰寫。
